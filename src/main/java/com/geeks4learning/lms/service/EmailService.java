package com.geeks4learning.lms.service;

import com.geeks4learning.lms.model.LeaveRequest;
import com.geeks4learning.lms.model.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.mail.MailException;

import jakarta.mail.internet.MimeMessage;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Async
    public void sendLeaveApprovalEmail(User employee, LeaveRequest request, User manager, String comments) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(employee.getEmail()); // Add email field to User model
            helper.setSubject(String.format("Leave Request #%d %s", request.getId(), request.getStatus()));

            // Prepare template context
            Context context = new Context();
            context.setVariable("employeeName", employee.getName());
            context.setVariable("status", request.getStatus());
            context.setVariable("requestId", request.getId());
            context.setVariable("leaveType", request.getLeaveType());
            context.setVariable("startDate", request.getStartDate().toString());
            context.setVariable("endDate", request.getEndDate().toString());
            context.setVariable("workingDays", request.getWorkingDays());
            context.setVariable("reason", request.getReason());
            context.setVariable("comments", comments);
            context.setVariable("managerName", manager.getName());

            // Process template
            String htmlContent = templateEngine.process("email/leave-approval", context);
            helper.setText(htmlContent, true);

            mailSender.send(message);
            log.info("✅ HTML email sent successfully to: {} - Request #{}", employee.getEmail(), request.getId());

        } catch (Exception e) {
            log.error("❌ Failed to send HTML email to: {}. Error: {}", employee.getEmail(), e.getMessage());
            // Fallback to simple email
            sendSimpleEmail(employee, request, manager, comments);
        }
    }

    private void sendSimpleEmail(User employee, LeaveRequest request, User manager, String comments) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(employee.getEmail());
            message.setSubject(String.format("Leave Request #%d %s", request.getId(), request.getStatus()));

            String body = String.format(
                    "Your %s leave request (%s to %s) has been %s by %s.\n\nWorking Days: %d\nReason: %s\nComments: %s",
                    request.getLeaveType(),
                    request.getStartDate(),
                    request.getEndDate(),
                    request.getStatus().toLowerCase(),
                    manager.getName(),
                    request.getWorkingDays(),
                    request.getReason(),
                    comments != null ? comments : "None"
            );

            message.setText(body);
            mailSender.send(message);
            log.info("✅ Simple email sent successfully to: {}", employee.getEmail());
        } catch (Exception ex) {
            log.error("❌ Failed to send simple email. Error: {}", ex.getMessage());
        }
    }

    @Async
    public void sendEmailNotification(String to, String subject, String body) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(to);
            message.setSubject(subject);
            message.setText(body);

            mailSender.send(message);
            log.info("✅ Email sent successfully to: {} - Subject: {}", to, subject);
        } catch (Exception e) {
            log.error("❌ Failed to send email to: {}. Error: {}", to, e.getMessage());
            // Fallback to console logging
            log.info("[EMAIL FALLBACK] To: {} | Subject: {} | {}", to, subject, body);
        }
    }
}