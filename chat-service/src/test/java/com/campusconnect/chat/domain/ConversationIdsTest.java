package com.campusconnect.chat.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ConversationIdsTest {

    @Test
    void deveGerarIdCanonicoIndependenteDaOrdemDosUsuarios() {
        assertThat(ConversationIds.of(10L, 2L)).isEqualTo("2_10");
        assertThat(ConversationIds.of(2L, 10L)).isEqualTo("2_10");
    }

    @Test
    void deveGerarIdQuandoIdsSaoIguais() {
        assertThat(ConversationIds.of(7L, 7L)).isEqualTo("7_7");
    }
}
