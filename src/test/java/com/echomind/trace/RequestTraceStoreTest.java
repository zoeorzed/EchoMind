package com.echomind.trace;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RequestTraceStoreTest {

    @Test
    void normalizesTimestampAndReturnsNewestTraceFirst() {
        RequestTraceStore store = new RequestTraceStore();
        store.record(trace("first", ""));
        store.record(trace("second", "2026-09-01T00:00:00Z"));

        assertThat(store.find("first")).get().extracting(RequestToolTrace::timestamp).isNotNull();
        assertThat(store.recent(2)).extracting(RequestToolTrace::requestId)
                .containsExactly("second", "first");
    }

    @Test
    void keepsOnlyTheMostRecentTwoHundredTraces() {
        RequestTraceStore store = new RequestTraceStore();
        for (int i = 0; i < 205; i++) {
            store.record(trace("req-" + i, "2026-09-01T00:00:00Z"));
        }

        assertThat(store.recent(500)).hasSize(200);
        assertThat(store.find("req-0")).isEmpty();
        assertThat(store.find("req-204")).isPresent();
    }

    @Test
    void updatesFinalEscalationWithoutDuplicatingTrace() {
        RequestTraceStore store = new RequestTraceStore();
        store.record(trace("req-final", "2026-09-01T00:00:00Z"));

        store.updateEscalated("req-final", true);

        assertThat(store.find("req-final")).get().extracting(RequestToolTrace::escalated).isEqualTo(true);
        assertThat(store.recent(10)).hasSize(1);
    }

    private RequestToolTrace trace(String requestId, String timestamp) {
        return new RequestToolTrace(
                requestId,
                timestamp,
                "chat",
                "user",
                "conversation",
                "query",
                "query",
                "general",
                "general",
                List.of(),
                List.of(),
                List.of(),
                false,
                false,
                10
        );
    }
}
