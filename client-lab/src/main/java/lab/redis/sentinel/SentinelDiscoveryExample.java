package lab.redis.sentinel;

import io.lettuce.core.RedisClient;
import io.lettuce.core.api.StatefulRedisConnection;
import lab.redis.common.Environment;

public final class SentinelDiscoveryExample {
    private SentinelDiscoveryExample() {
    }

    public static void run() {
        RedisClient client = RedisClient.create(Environment.sentinelUri());
        try (client; StatefulRedisConnection<String, String> connection = client.connect()) {
            var commands = connection.sync();
            String key = "lab:java:sentinel-discovery";
            commands.set(key, "ok");
            String value = commands.get(key);
            System.out.println("Sentinel-discovered primary value=" + value);
            System.out.println(commands.info("replication"));
            commands.del(key);
            if (!"ok".equals(value)) {
                throw new IllegalStateException("Sentinel-discovered write/read failed");
            }
        }
    }
}
