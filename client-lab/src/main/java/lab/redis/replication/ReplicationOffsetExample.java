package lab.redis.replication;

import io.lettuce.core.RedisClient;
import io.lettuce.core.api.StatefulRedisConnection;
import lab.redis.common.Environment;

public final class ReplicationOffsetExample {
    private ReplicationOffsetExample() {
    }

    public static void run() {
        RedisClient primaryClient = RedisClient.create(
                Environment.directUri("REDIS_PRIMARY_URI", "redis://redis-primary:6379"));
        try (primaryClient; StatefulRedisConnection<String, String> connection = primaryClient.connect()) {
            var commands = connection.sync();
            commands.set("lab:java:replication", "written-on-primary");
            long acknowledgements = commands.waitForReplication(2, 5_000);
            String info = commands.info("replication");
            System.out.printf("replicaAcknowledgements=%d%n", acknowledgements);
            System.out.println(info);
            if (acknowledgements < 2) {
                throw new IllegalStateException("Expected two replica acknowledgements");
            }
            commands.del("lab:java:replication");
        }
    }
}
