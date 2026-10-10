package com.example.app.common.mail;

import com.example.app.config.AppProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

/**
 * Single exit point for outgoing mail. With {@code app.mail.enabled=false} ({@code MAIL_ENABLED}) nothing is
 * sent and only a line without addresses, subject or body is logged (tokens travel inside the body).
 */
@Component
public class MailDispatcher {

    private static final Logger log = LoggerFactory.getLogger(MailDispatcher.class);

    private final JavaMailSender mailSender;
    private final AppProperties properties;

    public MailDispatcher(JavaMailSender mailSender, AppProperties properties) {
        this.mailSender = mailSender;
        this.properties = properties;
    }

    /** Throws the sender's runtime exception on delivery failure; callers decide how to log it. */
    public void send(SimpleMailMessage message) {
        if (!properties.mail().enabled()) {
            log.info("Mail delivery disabled (MAIL_ENABLED=false): one message not sent");
            return;
        }
        mailSender.send(message);
    }
}
