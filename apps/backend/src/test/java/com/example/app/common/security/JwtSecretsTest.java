package com.example.app.common.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

import java.util.Base64;
import javax.crypto.SecretKey;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.api.Test;

class JwtSecretsTest {

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    void missingSecretFailsWithClearMessage(String secret) {
        assertThatIllegalStateException()
                .isThrownBy(() -> JwtSecrets.hmacSha256Key(secret))
                .withMessageContaining("JWT_SECRET")
                .withMessageContaining("not set");
    }

    @Test
    void nonBase64SecretFails() {
        assertThatIllegalStateException()
                .isThrownBy(() -> JwtSecrets.hmacSha256Key("this is not base64 !!!"))
                .withMessageContaining("Base64");
    }

    @Test
    void secretShorterThan32BytesFails() {
        String shortSecret = Base64.getEncoder().encodeToString(new byte[31]);

        assertThatIllegalStateException()
                .isThrownBy(() -> JwtSecrets.hmacSha256Key(shortSecret))
                .withMessageContaining("too short")
                .withMessageContaining("31 bytes")
                .withMessageContaining("at least 32 bytes");
    }

    @Test
    void secretOf32BytesIsAccepted() {
        byte[] raw = new byte[32];
        raw[0] = 7;

        SecretKey key = JwtSecrets.hmacSha256Key(Base64.getEncoder().encodeToString(raw) + "\n");

        assertThat(key.getAlgorithm()).isEqualTo("HmacSHA256");
        assertThat(key.getEncoded()).isEqualTo(raw);
    }
}
