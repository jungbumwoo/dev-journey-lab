package lab.redis.common;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FailuresTest {
    @Test
    void reportsTheDeepestCauseForOperationalLogs() {
        RuntimeException failure = new RuntimeException(
                "client wrapper",
                new IllegalStateException("redis unavailable")
        );

        assertEquals(
                "IllegalStateException: redis unavailable",
                Failures.rootMessage(failure)
        );
    }
}
