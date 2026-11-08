package org.example.lamsserver;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@SpringBootApplication
@EnableTransactionManagement
@ComponentScan(basePackages = "org.example")
@MapperScan("org.example.lamsserver.mapper")
public class LamsServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(LamsServerApplication.class, args);
    }
}
