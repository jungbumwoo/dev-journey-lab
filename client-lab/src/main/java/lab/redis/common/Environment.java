package lab.redis.common;

import io.lettuce.core.RedisURI;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class Environment {
    private static final Duration COMMAND_TIMEOUT = Duration.ofSeconds(2);

    private Environment() {
    }

    public static String value(String name, String defaultValue) {
        String value = System.getenv(name);
        return value == null || value.isBlank() ? defaultValue : value;
    }

    public static RedisURI directUri(String environmentName, String defaultUri) {
        return RedisURI.create(value(environmentName, defaultUri));
    }

    public static List<RedisURI> clusterUris() {
        String nodes = value("REDIS_CLUSTER_NODES",
                "redis-cluster-1:6379,redis-cluster-2:6379,redis-cluster-3:6379");
        List<RedisURI> uris = new ArrayList<>();
        for (String node : nodes.split(",")) {
            String[] hostPort = splitHostPort(node);
            uris.add(RedisURI.Builder.redis(hostPort[0], Integer.parseInt(hostPort[1]))
                    .withTimeout(COMMAND_TIMEOUT)
                    .build());
        }
        return uris;
    }

    public static RedisURI sentinelUri() {
        String masterName = value("SENTINEL_MASTER_NAME", "redis-main");
        String nodes = value("SENTINEL_NODES", "sentinel-1:26379,sentinel-2:26379,sentinel-3:26379");
        List<String> sentinels = Arrays.asList(nodes.split(","));
        String[] first = splitHostPort(sentinels.getFirst());
        RedisURI.Builder builder = RedisURI.Builder
                .sentinel(first[0], Integer.parseInt(first[1]), masterName)
                .withTimeout(COMMAND_TIMEOUT);
        for (int index = 1; index < sentinels.size(); index++) {
            String[] hostPort = splitHostPort(sentinels.get(index));
            builder.withSentinel(hostPort[0], Integer.parseInt(hostPort[1]));
        }
        return builder.build();
    }

    private static String[] splitHostPort(String endpoint) {
        String[] parts = endpoint.trim().split(":", 2);
        if (parts.length != 2) {
            throw new IllegalArgumentException("Expected host:port, got: " + endpoint);
        }
        return parts;
    }
}
