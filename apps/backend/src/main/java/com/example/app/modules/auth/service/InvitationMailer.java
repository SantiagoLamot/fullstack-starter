package com.example.app.modules.auth.service;

import com.example.app.config.AppProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.SimpleMailMessage;
import com.example.app.common.mail.MailDispatcher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.web.util.UriComponentsBuilder;

/** Sends the invitation mail once the transaction that created the token has committed. */
@Component
public class InvitationMailer {

    private static final Logger log = LoggerFactory.getLogger(InvitationMailer.class);

    private final MailDispatcher mailSender;
    private final AppProperties properties;

    public InvitationMailer(MailDispatcher mailSender, AppProperties properties) {
        this.mailSender = mailSender;
        this.properties = properties;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onInvitationIssued(InvitationIssuedEvent event) {
        String link = UriComponentsBuilder.fromUri(properties.publicWebUrl())
                .path("/activar")
                .queryParam("token", event.rawToken())
                .build().toUriString();
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(properties.mail().from());
        message.setTo(event.email());
        message.setSubject("Activá tu cuenta de " + properties.name());
        message.setText("Hola " + event.firstName() + ",\n\n"
                + "Te invitamos a " + properties.name() + ". Para activar tu cuenta y elegir tu contraseña entrá a:\n\n"
                + link + "\n\n"
                + "El enlace es de un solo uso y vence en "
                + properties.security().invitationTtl().toHours() + " horas.\n");
        try {
            mailSender.send(message);
        } catch (RuntimeException ex) {
            // The user exists; an admin can resend the invitation later. Never log the token or the address.
            log.error("Could not send the invitation mail ({})", ex.getClass().getSimpleName());
        }
    }
}
