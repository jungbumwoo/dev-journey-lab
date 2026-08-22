package lab.redis.cluster;

import io.lettuce.core.cluster.RedisClusterClient;
import io.lettuce.core.cluster.api.StatefulRedisClusterConnection;
import lab.redis.common.ClusterConnections;
import lab.redis.common.Environment;
import lab.redis.common.Failures;

import java.time.Duration;
import java.time.Instant;

public final class ClusterFailoverWorkload {
    private ClusterFailoverWorkload() {
    }

    public static void run(String[] args) throws InterruptedException {
        int durationSeconds = args.length == 0 ? 25 : Integer.parseInt(args[0]);
        String key = Environment.value("LAB_KEY", "lab:failover:1");
        Instant deadline = Instant.now().plus(Duration.ofSeconds(durationSeconds));
        long successes = 0;
        long failures = 0;
        long reconnects = 0;

        RedisClusterClient client = ClusterConnections.createClient();
        StatefulRedisClusterConnection<String, String> connection = null;
        try (client) {
            connection = client.connect();
            connection.sync().ping();
            System.out.println("WORKLOAD_READY key=" + key);

            while (Instant.now().isBefore(deadline)) {
                try {
                    connection.sync().incr(key);
                    successes++;
                    Thread.sleep(50);
                } catch (RuntimeException exception) {
                    failures++;
                    System.out.println("retryableFailure=" + Failures.rootMessage(exception));
                    connection.close();
                    connection = null;

                    for (int attempt = 1; attempt <= 3 && connection == null; attempt++) {
                        try {
                            client.reloadPartitions();
                            connection = client.connect();
                            reconnects++;
                        } catch (RuntimeException reconnectFailure) {
                            System.out.println("reconnectAttempt=" + attempt + " " +
                                    Failures.rootMessage(reconnectFailure));
                            Thread.sleep(100L * attempt);
                        }
                    }
                    if (connection == null) {
                        Thread.sleep(500);
                        connection = client.connect();
                        reconnects++;
                    }
                }
            }
        } finally {
            if (connection != null) {
                connection.close();
            }
        }

        System.out.printf("{\"scenario\":\"cluster-failover\",\"successes\":%d," +
                        "\"failures\":%d,\"reconnects\":%d}%n",
                successes, failures, reconnects);
        if (successes == 0) {
            throw new IllegalStateException("No writes succeeded during Cluster workload");
        }
    }
}
