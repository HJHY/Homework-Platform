package org.hjhy.homeworkplatform.config;

import com.fasterxml.jackson.databind.deser.std.DateDeserializers;
import org.hjhy.homeworkplatform.interceptor.ClassInterceptor;
import org.hjhy.homeworkplatform.interceptor.HomeworkInterceptor;
import org.hjhy.homeworkplatform.interceptor.LoginInterceptor;
import org.hjhy.homeworkplatform.interceptor.TokenInterceptor;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * web mvc配置
 *
 * @author yezhiqiu
 * @date 2021/07/29
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer, Jackson2ObjectMapperBuilderCustomizer {

    private final TokenInterceptor tokenInterceptor;
    private final LoginInterceptor loginInterceptor;
    private final ClassInterceptor classInterceptor;
    private final HomeworkInterceptor homeworkInterceptor;

    public WebMvcConfig(TokenInterceptor tokenInterceptor, LoginInterceptor loginInterceptor, ClassInterceptor classInterceptor, HomeworkInterceptor homeworkInterceptor) {
        this.tokenInterceptor = tokenInterceptor;
        this.loginInterceptor = loginInterceptor;
        this.classInterceptor = classInterceptor;
        this.homeworkInterceptor = homeworkInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        //token拦截器
        registry.addInterceptor(tokenInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns("/auth/code", "/auth/register", "/auth/login", "/auth/social/login/**")
                .excludePathPatterns("/doc.html", "/v3/api-docs/**", "/swagger*", "/webjars/**", "/favicon.ico")//swagger3资源没有token也要放行
                .excludePathPatterns("/homeworks/callback/**");

        //登录拦截器
        registry.addInterceptor(loginInterceptor)
                .addPathPatterns("/auth/login");
        //班级拦截器
        registry.addInterceptor(classInterceptor)
                .addPathPatterns("/classes/**")
                .excludePathPatterns("/classes", "/classes/**/join", "/classes/simpleInfo", "/classes/relation");
        //作业拦截器
        registry.addInterceptor(homeworkInterceptor)
                .addPathPatterns("/homeworks/**")
                .excludePathPatterns("/homeworks", "/homeworks/callback/**", "/homeworks/committed", "/homeworks/uncommitted", "/homeworks/myLaunch", "/homeworks/callback");
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowCredentials(true)
                .allowedHeaders("*")
                .allowedOriginPatterns("*")
                .allowedMethods("*");
    }

    @Override
    public void customize(Jackson2ObjectMapperBuilder jacksonObjectMapperBuilder) {
        var dateDeserializer = new DateDeserializers.DateDeserializer(DateDeserializers.DateDeserializer.instance, new SimpleDateFormat("yyyy-MM-dd HH:mm:ss"), null);
        jacksonObjectMapperBuilder.deserializerByType(Date.class, dateDeserializer);
    }
}
