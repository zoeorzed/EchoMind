package com.echomind.tool;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class CircuitBreakerTest {

    @Test
    void opensAtThresholdAndClosesAfterSuccessfulHalfOpenProbe() {
        CircuitBreaker breaker = new CircuitBreaker(2, Duration.ZERO);

        breaker.recordFailure();
        assertThat(breaker.state()).isEqualTo(CircuitState.CLOSED);

        breaker.recordFailure();
        assertThat(breaker.state()).isEqualTo(CircuitState.OPEN);
        assertThat(breaker.allow()).isTrue();
        assertThat(breaker.state()).isEqualTo(CircuitState.HALF_OPEN);

        breaker.recordSuccess();
        assertThat(breaker.state()).isEqualTo(CircuitState.CLOSED);
        assertThat(breaker.allow()).isTrue();
    }
}
