package org.hjhy.homeworkplatform.strategy.impl;

import com.aliyun.oss.OSS;
import com.aliyun.oss.common.utils.BinaryUtil;
import com.aliyun.oss.model.PolicyConditions;
import com.aliyun.oss.model.VoidResult;
import com.aliyun.tea.okhttp.OkHttpClientBuilder;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.apache.http.HttpStatus;
import org.hjhy.homeworkplatform.config.aliyun.AliCloudFCConfigProperties;
import org.hjhy.homeworkplatform.config.aliyun.OssUploadConfigProperties;
import org.hjhy.homeworkplatform.constant.HomeworkConst;
import org.hjhy.homeworkplatform.dto.FileUploadCallbackBodyDto;
import org.hjhy.homeworkplatform.dto.HomeworkPackagedFileCleanDto;
import org.hjhy.homeworkplatform.exception.BaseException;
import org.hjhy.homeworkplatform.generator.service.MessageService;
import org.hjhy.homeworkplatform.strategy.ObjectStorageStrategy;
import org.hjhy.homeworkplatform.utils.CommonUtils;
import org.hjhy.homeworkplatform.vo.FileDownloadVo;
import org.hjhy.homeworkplatform.vo.FileUploadVo;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.ObjectUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * @author HJHY
 * @date 2024/2/29 21:55
 */
@Service("ossStrategyImpl")
@Slf4j
public class OssStrategyImpl implements ObjectStorageStrategy {
    private final MessageService messageService;

    /*上传相关*/
    private final OssUploadConfigProperties ossConfigProperties;
    private final OSS ossClient;

    /*打包和下载相关*/
    private final com.aliyun.fc_open20210406.Client fcClient;
    private final AliCloudFCConfigProperties aliCloudFCConfigProperties;

    public OssStrategyImpl(MessageService messageService, OssUploadConfigProperties ossConfigProperties, OSS ossClient, com.aliyun.fc_open20210406.Client fcClient, AliCloudFCConfigProperties aliCloudFCConfigProperties) {
        this.messageService = messageService;
        this.ossConfigProperties = ossConfigProperties;
        this.ossClient = ossClient;
        this.fcClient = fcClient;
        this.aliCloudFCConfigProperties = aliCloudFCConfigProperties;
    }

    /**
     * 验证RSA
     *
     * @param content content
     * @param sign sign
     * @param publicKey publicKey
     * @return boolean
     */
    public static boolean doCheck(String content, byte[] sign, String publicKey) {
        try {
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            byte[] encodedKey = BinaryUtil.fromBase64String(publicKey);
            PublicKey pubKey = keyFactory.generatePublic(new X509EncodedKeySpec(encodedKey));
            java.security.Signature signature = java.security.Signature.getInstance("MD5withRSA");
            signature.initVerify(pubKey);
            signature.update(content.getBytes());
            return signature.verify(sign);

        } catch (Exception e) {
            log.error(e.getMessage());
        }

        return false;
    }

    @Override
    public FileUploadVo uploadHomework(String filePath, Integer homeworkId) throws Exception {
        String host = "https://" + ossConfigProperties.getBucketName() + "." + ossConfigProperties.getEndpoint();
        long expireEndTime = System.currentTimeMillis() + HomeworkConst.HOMEWORK_SUBMISSION_TOKEN_EXPIRE_TIME;
        Date expiration = new Date(expireEndTime);
        //这里可以把文件大小限制为更小的范围 PostObject请求最大可支持的文件大小为100MB，即CONTENT_LENGTH_RANGE为100*1024*1024。
        PolicyConditions policyConds = new PolicyConditions();
        policyConds.addConditionItem(PolicyConditions.COND_CONTENT_LENGTH_RANGE, 0, 104857600);
        //设置文件路径名称并签名
        policyConds.addConditionItem(PolicyConditions.COND_KEY, filePath);
        //设置回调并签名
        String homeworkCallback = homeworkCallback(ossConfigProperties.getCallbackUrl(), homeworkId);

        //通过过期时间和policy生成签名
        String postPolicy = ossClient.generatePostPolicy(expiration, policyConds);
        byte[] binaryData = postPolicy.getBytes(StandardCharsets.UTF_8);
        String encodedPolicy = BinaryUtil.toBase64String(binaryData);
        String postSignature = ossClient.calculatePostSignature(postPolicy);

        return FileUploadVo.builder().accessid(ossConfigProperties.getAccessKeyId()).policy(encodedPolicy).signature(postSignature).filePath(filePath).host(host).callback(homeworkCallback).expire(String.valueOf(expireEndTime / 1000)).build();
    }

    @Override
    public boolean exists(String filePath) {
        return ossClient.doesObjectExist(ossConfigProperties.getBucketName(), filePath);
    }

    @Override
    public void deleteHomework(String filePath) {
        VoidResult voidResult = ossClient.deleteObject(ossConfigProperties.getBucketName(), filePath);
        log.info("删除文件结果:{}", voidResult.toString());
    }

    /**
     * 生成用Base64处理后的回调请求体
     * @param callbackUrl callback请求的url
     * @return base64处理后的回调请求体
     * @throws Exception exception
     */
    private String homeworkCallback(String callbackUrl, Integer homeworkId) throws Exception {
        String callbackBody = "{\"mimeType\":${mimeType},\"size\":${size},\"homeworkId\":%d}".formatted(homeworkId);

        HashMap<String, Object> callbackMap = new HashMap<>();
        callbackMap.put("callbackUrl", callbackUrl);
        callbackMap.put("callbackBodyType", "application/json");
        callbackMap.put("callbackBody", callbackBody);
        String jasonCallback = CommonUtils.getObjectMapper().writeValueAsString(callbackMap);

        return BinaryUtil.toBase64String(jasonCallback.getBytes());
    }

    @Override
    @Transactional
    public boolean verifyOSSCallbackRequest(FileUploadCallbackBodyDto.AliCloud callbackBody, HttpServletRequest request) throws Exception {
        String authorizationInput = request.getHeader("Authorization");
        String pubKeyInput = request.getHeader("x-oss-pub-key-url");
        byte[] authorization = BinaryUtil.fromBase64String(authorizationInput);
        byte[] pubKey = BinaryUtil.fromBase64String(pubKeyInput);
        String pubKeyAddr = new String(pubKey);
        if (!pubKeyAddr.startsWith("https://gosspublic.alicdn.com/") && !pubKeyAddr.startsWith("https://gosspublic.alicdn.com/")) {
            log.warn("pub key addr must be oss address");
            return false;
        }
        String retString = executeGet(pubKeyAddr);
        retString = retString.replace("-----BEGIN PUBLIC KEY-----", "");
        retString = retString.replace("-----END PUBLIC KEY-----", "");
        String queryString = request.getQueryString();
        String uri = request.getRequestURI();
        String authStr = java.net.URLDecoder.decode(uri, StandardCharsets.UTF_8);
        if (queryString != null && !queryString.isEmpty()) {
            authStr += "?" + queryString;
        }
        log.info("callbackBody:{}", callbackBody);
        authStr += "\n" + CommonUtils.getObjectMapper().writeValueAsString(callbackBody);
        log.info("开始对回调进行验证");
        return doCheck(authStr, authorization, retString);
    }

    /**
     * 获取public key
     *
     * @param url url
     * @return str
     */
    public String executeGet(String url) {
        BufferedReader in;

        String content = null;

        OkHttpClient okHttpClient = new OkHttpClientBuilder().buildOkHttpClient();
        Request request = new Request.Builder().url(url).get().build();

        try (Response response = okHttpClient.newCall(request).execute()) {
            ResponseBody body = response.body();
            if (body == null) {
                throw new IOException("response body is null");
            }
            in = new BufferedReader(new InputStreamReader(body.byteStream()));
            StringBuilder sb = new StringBuilder();
            String line;
            String NL = System.lineSeparator();
            while ((line = in.readLine()) != null) {
                sb.append(line).append(NL);
            }
            in.close();
            content = sb.toString();
        } catch (Exception e) {
            log.error(e.getMessage());
        }
        return content;
    }

    @Override
    @Transactional
    public void responseUploadCallback(HttpServletRequest request, HttpServletResponse response, boolean res) throws Exception {
        log.info("准备响应回调给OSS,验证结果为{}", res);
        if (res) {
            response(request, response, "{\"Status\":\"OK\"}", HttpServletResponse.SC_OK);
        } else {
            response(request, response, "{\"Status\":\"verify not ok\"}", HttpServletResponse.SC_BAD_REQUEST);
        }
    }

    /**
     * 服务器响应结果
     *
     * @param request request
     * @param response response
     * @param results results
     * @param status status
     * @throws IOException exception
     */
    private void response(HttpServletRequest request, HttpServletResponse response, String results, int status) throws IOException {
        String callbackFunName = request.getParameter("callback");
        response.addHeader("Content-Length", String.valueOf(results.length()));
        if (callbackFunName == null || callbackFunName.equalsIgnoreCase("")) response.getWriter().println(results);
        else response.getWriter().println(callbackFunName + "( " + results + " )");
        response.setStatus(status);
        response.flushBuffer();
    }

    @Override
    public FileDownloadVo downloadHomework(String sourceDir, String destFilePath) throws Exception {
        //使用函数计算进行打包(这里的参数其实只要和函数计算具体逻辑参数定义一样就行了,函数计算的逻辑是可以自己修改的!!)
        Map<String, String> params = new HashMap<>();
        params.put("bucket", aliCloudFCConfigProperties.getBucketName());
        params.put("source-dir", sourceDir);
        //定义文件的路径
        params.put("dest-file", destFilePath);

        var jsonParams = CommonUtils.getObjectMapper().writeValueAsString(params);
        System.out.println("JSON:" + jsonParams);

        Request request = new Request.Builder()
                .url(aliCloudFCConfigProperties.getUrl())
                .post(RequestBody.create(jsonParams, MediaType.parse("application/json")))
                .build();

        // 对请求进行签名(保证函数计算的调用者拥有密钥)
        var okHttpClient = new OkHttpClientBuilder().buildOkHttpClient();
        try {
            request = fcClient.SignRequest(request);
        } catch (Exception e) {
            log.error("FC进行请求签名发生异常:{}", e.getMessage());
            throw new BaseException("FC进行签名发生异常");
        }

        var fileDownloadVo = new FileDownloadVo();
        try (Response res = okHttpClient.newCall(request).execute()) {
            if (res.code() != HttpStatus.SC_OK) {
                log.warn("FC请求状态码为{}", res.code());
                throw new IOException("FC请求状态码不为200");
            }
            //从响应中获取数据
            if (ObjectUtils.isEmpty(res.request())) {
                log.info("res.request() is null");
                throw new BaseException("FC请求返回体中request为空");
            }
            if (ObjectUtils.isEmpty(res.request().url())) {
                throw new IOException("FC响应中中url为空");
            }
            /*
             * 设置下载的签名url
             * FC运行的服务默认设置的url时间是5分钟,和本项目设置的时间一样,因此没有传递参数
             * 如果需要则应该自行在FC和请求之间进行参数传递
             * */
            fileDownloadVo.setUrl(res.request().url().toString());
        } catch (IOException e) {
            log.error("FC进行请求发送发生异常:{}", e.getMessage());
            throw new BaseException("FC进行请求时发生异常");
        }
        //一段时间后进行打包文件的清理工作
        messageService.cleanPackagedFile(new HomeworkPackagedFileCleanDto(aliCloudFCConfigProperties.getBucketName(), destFilePath));

        return fileDownloadVo;
    }

}
