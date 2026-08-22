package lab.redis.cluster;

import io.lettuce.core.RedisCommandExecutionException;
import io.lettuce.core.ScriptOutputType;
import io.lettuce.core.cluster.RedisClusterClient;
import io.lettuce.core.cluster.SlotHash;
import io.lettuce.core.cluster.api.StatefulRedisClusterConnection;
import lab.redis.common.ClusterConnections;

import java.util.List;

public final class ClusterCrossSlotExample {
    private static final String SCRIPT = "return {KEYS[1], KEYS[2]}";

    private ClusterCrossSlotExample() {
    }

    public static void run() {
        String first = "order:1";
        String second = "order:2";
        String taggedFirst = "order:{customer-1}:1";
        String taggedSecond = "order:{customer-1}:2";

        RedisClusterClient client = ClusterConnections.createClient();
        try (client; StatefulRedisClusterConnection<String, String> connection = client.connect()) {
            var commands = connection.sync();
            String crossSlotError;
            try {
                commands.eval(SCRIPT, ScriptOutputType.MULTI, new String[]{first, second});
                throw new IllegalStateException("Cross-slot EVAL unexpectedly succeeded");
            } catch (RedisCommandExecutionException exception) {
                if (!exception.getMessage().contains("CROSSSLOT")) {
                    throw exception;
                }
                crossSlotError = exception.getMessage();
            }

            List<?> result = commands.eval(SCRIPT, ScriptOutputType.MULTI,
                    new String[]{taggedFirst, taggedSecond});
            System.out.printf("differentSlots=%d,%d error=%s%n",
                    SlotHash.getSlot(first), SlotHash.getSlot(second), crossSlotError);
            System.out.printf("taggedSlots=%d,%d result=%s%n",
                    SlotHash.getSlot(taggedFirst), SlotHash.getSlot(taggedSecond), result);

            if (SlotHash.getSlot(taggedFirst) != SlotHash.getSlot(taggedSecond)) {
                throw new IllegalStateException("Hash-tagged keys must share a slot");
            }
        }
    }
}
