package lab.redis.cluster;

import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisCommandExecutionException;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.cluster.RedisClusterClient;
import io.lettuce.core.cluster.api.StatefulRedisClusterConnection;
import lab.redis.common.ClusterConnections;
import lab.redis.common.Environment;

public final class ClusterRedirectExample {
    private ClusterRedirectExample() {
    }

    public static void run(String[] args) {
        String key = args.length == 0 ? "lab:moved:1" : args[0];
        RedisClient directClient = RedisClient.create(
                Environment.directUri("REDIS_DIRECT_CLUSTER_URI", "redis://redis-cluster-1:6379"));

        String moved;
        try (directClient; StatefulRedisConnection<String, String> direct = directClient.connect()) {
            try {
                direct.sync().get(key);
                throw new IllegalStateException("The selected key is local; expected a MOVED response: " + key);
            } catch (RedisCommandExecutionException exception) {
                if (!exception.getMessage().startsWith("MOVED")) {
                    throw exception;
                }
                moved = exception.getMessage();
            }
        }

        RedisClusterClient clusterClient = ClusterConnections.createClient();
        try (clusterClient;
             StatefulRedisClusterConnection<String, String> cluster = clusterClient.connect()) {
            String result = cluster.sync().set(key, "written-by-lettuce-cluster-client");
            String value = cluster.sync().get(key);
            System.out.println("directClient=" + moved);
            System.out.println("clusterAwareClient=" + result + ", value=" + value);
            if (!"OK".equals(result) || !"written-by-lettuce-cluster-client".equals(value)) {
                throw new IllegalStateException("Cluster-aware write/read failed");
            }
        }
    }
}
