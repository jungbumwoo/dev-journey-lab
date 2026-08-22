package lab.redis;

import lab.redis.cluster.ClusterCrossSlotExample;
import lab.redis.cluster.ClusterFailoverWorkload;
import lab.redis.cluster.ClusterRedirectExample;
import lab.redis.replication.ReplicationOffsetExample;
import lab.redis.sentinel.SentinelDiscoveryExample;
import lab.redis.sentinel.SentinelFailoverWorkload;

import java.util.Arrays;

public final class LabApplication {
    private LabApplication() {
    }

    public static void main(String[] args) throws Exception {
        if (args.length == 0) {
            usage();
            System.exit(2);
        }

        String scenario = args[0];
        String[] scenarioArgs = Arrays.copyOfRange(args, 1, args.length);
        switch (scenario) {
            case "replication-offset" -> ReplicationOffsetExample.run();
            case "sentinel-discovery" -> SentinelDiscoveryExample.run();
            case "sentinel-failover" -> SentinelFailoverWorkload.run(scenarioArgs);
            case "cluster-redirect" -> ClusterRedirectExample.run(scenarioArgs);
            case "cluster-crossslot" -> ClusterCrossSlotExample.run();
            case "cluster-failover" -> ClusterFailoverWorkload.run(scenarioArgs);
            default -> {
                System.err.println("Unknown scenario: " + scenario);
                usage();
                System.exit(2);
            }
        }
    }

    private static void usage() {
        System.err.println("Scenarios: replication-offset, sentinel-discovery, sentinel-failover, " +
                "cluster-redirect, cluster-crossslot, cluster-failover");
    }
}
