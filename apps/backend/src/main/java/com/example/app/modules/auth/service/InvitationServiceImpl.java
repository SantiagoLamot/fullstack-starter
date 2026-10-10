package com.example.app.modules.auth.service;

import com.example.app.common.exception.BusinessException;
import com.example.app.config.AppProperties;
import com.example.app.modules.auth.entity.InvitationToken;
import com.example.app.modules.auth.repository.InvitationTokenRepository;
import com.example.app.modules.users.service.UserAccountService;
import java.time.Clock;
import java.time.Instant;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InvitationServiceImpl implements InvitationService {

    static final String INVALID_INVITATION = "La invitación es inválida o ya venció.";

    private final InvitationTokenRepository repository;
    private final OpaqueTokens opaqueTokens;
    private final UserAccountService userAccountService;
    private final PasswordEncoder passwordEncoder;
    private final ApplicationEventPublisher events;
    private final AppProperties properties;
    private final Clock clock;

    public InvitationServiceImpl(InvitationTokenRepository repository, OpaqueTokens opaqueTokens,
            UserAccountService userAccountService, PasswordEncoder passwordEncoder,
            ApplicationEventPublisher events, AppProperties properties, Clock clock) {
        this.repository = repository;
        this.opaqueTokens = opaqueTokens;
        this.userAccountService = userAccountService;
        this.passwordEncoder = passwordEncoder;
        this.events = events;
        this.properties = properties;
        this.clock = clock;
    }

    @Override
    @Transactional
    public void issue(Long userId, String email, String firstName) {
        Instant now = clock.instant();
        repository.findByUserIdAndUsedAtIsNull(userId).forEach(token -> token.markUsed(now));
        String raw = opaqueTokens.generate();
        repository.save(new InvitationToken(userId, opaqueTokens.hash(raw),
                now.plus(properties.security().invitationTtl())));
        events.publishEvent(new InvitationIssuedEvent(email, firstName, raw));
    }

    @Override
    @Transactional
    public void accept(String rawToken, String rawPassword) {
        Instant now = clock.instant();
        InvitationToken token = repository.findByTokenHash(opaqueTokens.hash(rawToken))
                .filter(found -> found.isUsable(now))
                .orElseThrow(() -> new BusinessException(INVALID_INVITATION));
        userAccountService.activate(token.getUserId(), passwordEncoder.encode(rawPassword));
        token.markUsed(now);
    }
}
