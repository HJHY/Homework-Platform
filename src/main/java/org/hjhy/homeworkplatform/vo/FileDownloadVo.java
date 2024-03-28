package org.hjhy.homeworkplatform.vo;

import lombok.*;

/**
 * 目前考虑的方式采用签名url直传的方式,不适用sts
 * @author HJHY
 * @date 2024/2/26 18:52
 */
@NoArgsConstructor
@AllArgsConstructor
@Data
@Builder
@ToString
public class FileDownloadVo {
    /*文件url*/
    private String url;
}
