package com.example.app.common.mail;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.example.app.config.AppProperties;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

class MailDispatcherTest {

    private final JavaMailSender sender = mock(JavaMailSender.class);

    @Test
    void sendsWhenEnabled() {
        SimpleMailMessage message = new SimpleMailMessage();
        dispatcher(true).send(message);
        verify(sender).send(message);
    }

    @Test
    void sendsNothingWhenDisabled() {
        dispatcher(false).send(new SimpleMailMessage());
        verify(sender, never()).send(ArgumentMatchers.any(SimpleMailMessage.class));
    }

    private MailDispatcher dispatcher(boolean enabled) {
        AppProperties properties = new AppProperties(null, null, null, null,
                new AppProperties.Mail("no-reply@example.local", null, enabled), null, null, null);
        return new MailDispatcher(sender, properties);
    }
}
