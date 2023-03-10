package com.nene;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * @author Ayachi Nene
 */
@SpringBootApplication
@MapperScan(basePackages = {"com.nene.mapper"})
public class NeneBlogApplication {
    public static void main(String[] args) {
        SpringApplication.run(NeneBlogApplication.class, args);
    }
}
