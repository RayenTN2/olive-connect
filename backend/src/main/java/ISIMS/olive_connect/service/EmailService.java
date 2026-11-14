package ISIMS.olive_connect.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private final JavaMailSender mailSender;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void sendVerificationEmail(String toEmail, String verificationCode) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            // true = multipart (allows HTML content)
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom("support@oliveconnect.tn", "Olive-Connect Cooperative");
            helper.setTo(toEmail);
            helper.setSubject("🌿 Verify Your Olive-Connect Account");

            String htmlContent = buildEmailTemplate(verificationCode);
            helper.setText(htmlContent, true); // true enables HTML parsing

            mailSender.send(message);
        } catch (Exception e) {
            throw new RuntimeException("Failed to send verification email: " + e.getMessage());
        }
    }

    private String buildEmailTemplate(String code) {
        return "<div style=\"font-family: Arial, sans-serif; background-color: #f4f7f4; padding: 30px; margin: 0;\">" +
                "  <div style=\"max-width: 600px; background-color: #ffffff; padding: 40px; border-radius: 10px; box-shadow: 0 4px 12px rgba(0,0,0,0.05); margin: 0 auto;\">" +
                "    " +
                "    <!-- Header -->" +
                "    <div style=\"text-align: center; border-bottom: 2px solid #eef2ed; padding-bottom: 20px; margin-bottom: 30px;\">" +
                "      <h1 style=\"color: #2c5e3b; margin: 0; font-size: 26px;\">Olive-Connect</h1>" +
                "      <p style=\"color: #7f8c8d; font-size: 14px; margin: 5px 0 0 0;\">Smart Olive Cooperative Management - Sfax</p>" +
                "    </div>" +
                "    " +
                "    <!-- Body Content -->" +
                "    <h2 style=\"color: #333333; font-size: 20px; margin-top: 0;\">Welcome to the Cooperative!</h2>" +
                "    <p style=\"color: #555555; font-size: 15px; line-height: 1.6;\">" +
                "      Thank you for registering as a partner farmer. To complete your registration request and verify your email address, please use the secure verification code below:" +
                "    </p>" +
                "    " +
                "    <!-- Code Box -->" +
                "    <div style=\"text-align: center; margin: 35px 0;\">" +
                "      <span style=\"display: inline-block; background-color: #f0f7f4; color: #2c5e3b; font-size: 32px; font-weight: bold; letter-spacing: 6px; padding: 15px 30px; border-radius: 8px; border: 1px dashed #b2d8b2;\">" +
                "        " + code + "" +
                "      </span>" +
                "    </div>" +
                "    " +
                "    <p style=\"color: #777777; font-size: 13px; line-height: 1.5; text-align: center;\">" +
                "      This code will expire in <strong>15 minutes</strong>. If you did not request this registration, please ignore this email." +
                "    </p>" +
                "    " +
                "    <!-- Footer -->" +
                "    <div style=\"border-top: 1px solid #eef2ed; margin-top: 30px; padding-top: 20px; text-align: center;\">" +
                "      <p style=\"color: #999999; font-size: 12px; margin: 0;\">" +
                "        &copy; 2026 Olive-Connect Cooperative. All rights reserved.<br>" +
                "        Institut Supérieur d'Informatique et de Multimédia de Sfax (ISIMS)" +
                "      </p>" +
                "    </div>" +
                "    " +
                "  </div>" +
                "</div>";
    }
}