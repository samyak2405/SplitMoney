package com.splitwise.notification.service.email;

import com.splitwise.notification.config.NotificationEmailProperties;
import com.splitwise.notification.exception.custom.email.PermanentEmailException;
import com.splitwise.notification.exception.custom.email.TransientEmailException;
import com.splitwise.notification.messaging.NotificationMessage;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.MailAuthenticationException;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "notification.email.provider", havingValue = "gmail", matchIfMissing = true)
public class GmailSmtpEmailGateway implements EmailGateway {
    private final JavaMailSender mailSender;
    private final NotificationEmailProperties emailProperties;
    private final EmailTemplateService templateService;

    public GmailSmtpEmailGateway(
            JavaMailSender mailSender,
            NotificationEmailProperties emailProperties,
            EmailTemplateService templateService
    ) {
        this.mailSender = mailSender;
        this.emailProperties = emailProperties;
        this.templateService = templateService;
    }

    @Override
    public void send(NotificationMessage message) throws TransientEmailException, PermanentEmailException {
        String toEmail = resolveToEmail(message);
        ResolvedEmailTemplate template = templateService.resolve(message.eventType(), message.payload());
        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, false, "UTF-8");
            helper.setFrom(resolveFromAddress());
            helper.setTo(toEmail);
            helper.setSubject(template.subject());
            helper.setText(template.body(), template.html());
            mailSender.send(mimeMessage);
        } catch (MailAuthenticationException authenticationException) {
            throw new PermanentEmailException("SMTP authentication failed. Check Gmail app password.");
        } catch (MailSendException sendException) {
            throw new TransientEmailException("Email provider send error: " + sendException.getMessage());
        } catch (MessagingException messagingException) {
            throw new PermanentEmailException("Invalid email message format: " + messagingException.getMessage());
        } catch (Exception exception) {
            throw new TransientEmailException("Unexpected email provider failure: " + exception.getMessage());
        }
    }

    private String resolveToEmail(NotificationMessage message) {
        Object email = message.payload().get("email");
        if (email == null || String.valueOf(email).isBlank()) {
            throw new PermanentEmailException("No recipient email found in payload.email");
        }
        return String.valueOf(email);
    }

    private String resolveFromAddress() {
        if (emailProperties.getFromAddress() != null && !emailProperties.getFromAddress().isBlank()) {
            return emailProperties.getFromAddress();
        }
        throw new PermanentEmailException("Missing notification.email.from-address configuration");
    }
}
