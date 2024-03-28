package org.hjhy.homeworkplatform;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/*通过exclude可以在启动时去掉自动装配*/
@SpringBootApplication
@MapperScan("org.hjhy.homeworkplatform.generator.mapper")
@EnableScheduling
public class HomeworkPlatformApplication {

    public static void main(String[] args) {
        SpringApplication.run(HomeworkPlatformApplication.class, args);
    }

}
