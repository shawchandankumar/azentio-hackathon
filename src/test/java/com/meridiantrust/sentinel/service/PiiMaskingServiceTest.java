package com.meridiantrust.sentinel.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class PiiMaskingServiceTest {

    private final PiiMaskingService masking = new PiiMaskingService();

    @Test
    void masksEachNameToken() {
        assertThat(masking.maskName("John Doe")).isEqualTo("J*** D**");
        assertThat(masking.maskName("Aisha")).isEqualTo("A****");
    }

    @Test
    void masksIdKeepingLastThree() {
        assertThat(masking.maskId("AADH1234567")).isEqualTo("********567");
    }

    @Test
    void nullSafe() {
        assertThat(masking.maskName(null)).isNull();
        assertThat(masking.maskId(null)).isNull();
    }
}
