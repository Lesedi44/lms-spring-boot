package com.geeks4learning.lms.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class EmailService {

    @Async
    public void sendEmailNotification(String to, String subject, String body) {
        // Simulate email sending
        log.info("[EMAIL] To: {} | Subject: {} | {}", to, subject, body);
    }
}