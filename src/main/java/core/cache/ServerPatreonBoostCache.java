package core.cache;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import core.DiscordApiManager;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.javacord.api.entity.server.Server;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

public class ServerPatreonBoostCache {

    private static final ServerPatreonBoostCache ourInstance = new ServerPatreonBoostCache();

    private ServerPatreonBoostCache() {
    }

    public static ServerPatreonBoostCache getInstance() {
        return ourInstance;
    }

    private final LoadingCache<Long, Boolean> cache = CacheBuilder.newBuilder()
            .expireAfterAccess(Duration.ofMinutes(10))
            .build(
                    new CacheLoader<>() {
                        @Override
                        public Boolean load(@NonNull Long serverId) {
                            Optional<Server> serverOptional = DiscordApiManager.getInstance().getLocalServerById(serverId);
                            if (serverOptional.isPresent()) {
                                Server server = serverOptional.get();

                                return server.getMembers().stream()
                                        .filter(user -> !user.isBot() && server.canManage(user))
                                        .anyMatch(user -> PatreonCache.getInstance().getUserTier(user.getId()) > 1);
                            }

                            return false;
                        }
                    }
            );

    public void setTrue(long serverId) {
        cache.put(serverId, true);
    }

    public boolean get(long serverId) throws ExecutionException {
        return cache.get(serverId);
    }

}