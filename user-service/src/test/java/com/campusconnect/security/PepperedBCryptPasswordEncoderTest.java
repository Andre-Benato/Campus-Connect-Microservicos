package com.campusconnect.security;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class PepperedBCryptPasswordEncoderTest {

    private static final String SECRET = "secret-de-teste";
    private static final String RAW = "senha-super-secreta";

    private final PepperedBCryptPasswordEncoder encoder =
            new PepperedBCryptPasswordEncoder(SECRET, 4);

    @Test
    void deveGerarHashBcryptQueConfereComASenhaOriginal() {
        String hash = encoder.encode(RAW);

        assertThat(hash).startsWith("$2a$04$").isNotEqualTo(RAW);
        assertThat(encoder.matches(RAW, hash)).isTrue();
    }

    @Test
    void deveRecusarSenhaIncorreta() {
        String hash = encoder.encode(RAW);

        assertThat(encoder.matches("senha-errada", hash)).isFalse();
    }

    @Test
    void deveGerarHashesDiferentesParaAMesmaSenha() {
        assertThat(encoder.encode(RAW)).isNotEqualTo(encoder.encode(RAW));
    }

    @Test
    void naoDeveConferirQuandoOSecretMuda() {
        String hash = encoder.encode(RAW);
        PepperedBCryptPasswordEncoder outroSecret =
                new PepperedBCryptPasswordEncoder("outro-secret", 4);

        assertThat(outroSecret.matches(RAW, hash)).isFalse();
    }

    @Test
    void deveAceitarSenhasMaioresQue72Bytes() {
        String longa = "a".repeat(200);
        String hash = encoder.encode(longa);

        assertThat(encoder.matches(longa, hash)).isTrue();
        assertThat(encoder.matches("a".repeat(199), hash)).isFalse();
    }

    @Test
    void deveRetornarFalsoParaHashInvalido() {
        assertThat(encoder.matches(RAW, "")).isFalse();
        assertThat(encoder.matches(RAW, null)).isFalse();
    }
}
