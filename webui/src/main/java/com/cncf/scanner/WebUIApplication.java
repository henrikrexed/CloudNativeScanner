package com.cncf.scanner;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@EntityScan("com.cncf.scanner.model")
@EnableJpaRepositories("com.cncf.scanner.repository")
public class WebUIApplication {

    public static void main(String[] args) {
        SpringApplication.run(WebUIApplication.class, args);
    }
}


