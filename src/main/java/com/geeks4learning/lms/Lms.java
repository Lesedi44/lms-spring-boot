package com.geeks4learning.lms;


import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@EntityScan("com.geeks4learning.lms.model")  // Explicitly scan entities
@EnableJpaRepositories("com.geeks4learning.lms.repository")  // Explicitly scan repos
public class Lms {
    public static void main(String[] args) {
        SpringApplication.run(Lms.class, args);
    }
}