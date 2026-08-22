package lab.redis.sentinel;

import io.lettuce.core.RedisClient;
import io.lettuce.core.api.StatefulRedisConnection;
import lab.redis.common.Environment;
import lab.redis.common.Failures;

import java.time.Duration;
import java.time.Instant;

public final class SentinelFailoverWorkload {
    private SentinelFailoverWorkload() {
    }

    public static void run(String[] args) throws InterruptedException {
        int durationSeconds = args.length == 0 ? 25 : Integer.parseInt(args[0]);
        Instant deadline = Instant.now().plus(Duration.ofSeconds(durationSeconds));
        String key = "lab:java:sentinel-failover";
        long successes = 0;
        long failures = 0;

        RedisClient initialClient = RedisClient.create(Environment.sentinelUri());
        try (initialClient; StatefulRedisConnection<String, String> initialConnection = initialClient.connect()) {
            initialConnection.sync().ping();
        }

        System.out.println("WORKLOAD_READY");
        while (Instant.now().isBefore(deadline)) {
            RedisClient attemptClient = RedisClient.create(Environment.sentinelUri());
            try (attemptClient; StatefulRedisConnection<String, String> attemptConnection = attemptClient.connect()) {
                attemptConnection.sync().incr(key);
                successes++;
                Thread.sleep(100);
            } catch (RuntimeException exception) {
                failures++;
                System.out.println("retryableFailure=" + Failures.rootMessage(exception));
                Thread.sleep(250);
            }
        }

        System.out.printf("{\"scenario\":\"sentinel-failover\",\"successes\":%d,\"failures\":%d}%n",
                successes, failures);
        if (successes == 0) {
            throw new IllegalStateException("No writes succeeded during the Sentinel workload");
        }
    }
}
