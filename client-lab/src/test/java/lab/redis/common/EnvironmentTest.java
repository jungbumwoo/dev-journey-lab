package lab.redis.common;

import io.lettuce.core.RedisURI;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class EnvironmentTest {
    @Test
    void createsADirectUriFromTheFallbackWhenVariableIsAbsent() {
        RedisURI uri = Environment.directUri(
                "DEV_JOURNEY_LAB_INTENTIONALLY_UNSET_URI",
                "redis://example-redis:6380/2"
        );

        assertEquals("example-redis", uri.getHost());
        assertEquals(6380, uri.getPort());
        assertEquals(2, uri.getDatabase());
    }
}
