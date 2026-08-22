package lab.redis.common;

import io.lettuce.core.cluster.ClusterClientOptions;
import io.lettuce.core.cluster.ClusterTopologyRefreshOptions;
import io.lettuce.core.cluster.RedisClusterClient;

import java.time.Duration;

public final class ClusterConnections {
    private ClusterConnections() {
    }

    public static RedisClusterClient createClient() {
        ClusterTopologyRefreshOptions refreshOptions = ClusterTopologyRefreshOptions.builder()
                .enablePeriodicRefresh(Duration.ofSeconds(5))
                .enableAllAdaptiveRefreshTriggers()
                .dynamicRefreshSources(true)
                .build();
        ClusterClientOptions clientOptions = ClusterClientOptions.builder()
                .autoReconnect(true)
                .validateClusterNodeMembership(true)
                .topologyRefreshOptions(refreshOptions)
                .build();

        RedisClusterClient client = RedisClusterClient.create(Environment.clusterUris());
        client.setOptions(clientOptions);
        return client;
    }
}
