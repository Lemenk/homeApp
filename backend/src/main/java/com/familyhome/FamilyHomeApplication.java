package com.familyhome;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.familyhome.mapper")
public class FamilyHomeApplication {

    public static void main(String[] args) {
        SpringApplication.run(FamilyHomeApplication.class, args);
    }
}
