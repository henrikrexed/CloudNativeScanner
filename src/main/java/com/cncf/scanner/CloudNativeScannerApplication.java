package com.cncf.scanner;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class CloudNativeScannerApplication {

    public static void main(String[] args) {
        SpringApplication.run(CloudNativeScannerApplication.class, args);
    }
}

