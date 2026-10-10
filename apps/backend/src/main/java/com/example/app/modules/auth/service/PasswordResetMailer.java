package com.example.app.modules.auth.service;

import com.example.app.common.mail.MailDispatcher;
import com.example.app.config.AppProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * Sends the password reset mail after the commit, on another thread: a synchronous SMTP call would make the
 * response slower when the email exists and allow enumerating accounts by timing.
 */
@Component
public class PasswordResetMailer {

    private static final Logger log = LoggerFactory.getLogger(PasswordResetMailer.class);

    private final MailDispatcher mailSender;
    private final AppProperties properties;

    public PasswordResetMailer(MailDispatcher mailSender, AppProperties properties) {
        this.mailSender = mailSender;
        this.properties = properties;
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onPasswordResetRequested(PasswordResetRequestedEvent event) {
        String link = UriComponentsBuilder.fromUri(properties.publicWebUrl())
                .path("/restablecer")
                .queryParam("token", event.rawToken())
                .build().toUriString();
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(properties.mail().from());
        message.setTo(event.email());
        message.setSubject("Restablecé tu contraseña de " + properties.name());
        message.setText("Hola " + event.firstName() + ",\n\n"
                + "Recibimos un pedido para restablecer la contraseña de tu cuenta de " + properties.name() + ". "
                + "Para elegir una contraseña nueva entrá a:\n\n"
                + link + "\n\n"
                + "El enlace es de un solo uso y vence en "
                + properties.security().passwordResetTtl().toMinutes() + " minutos.\n\n"
                + "Si no pediste este cambio, ignorá este mensaje: tu contraseña actual sigue funcionando.\n");
        try {
            mailSender.send(message);
        } catch (RuntimeException ex) {
            // The user can ask again. Never log the token or the address.
            log.error("Could not send the password reset mail ({})", ex.getClass().getSimpleName());
        }
    }
}
