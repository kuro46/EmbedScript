package shirokuro.embedscript.util;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.gson.reflect.TypeToken;
import shirokuro.embedscript.GsonHolder;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * @author shirokuro
 */
public final class MojangUtil {
    private static final Cache<UUID, String> NAME_CACHE = CacheBuilder.newBuilder()
        .expireAfterAccess(1, TimeUnit.HOURS)
        .build();

    private MojangUtil() {

    }

    public static String getName(UUID uniqueId) {
        String cached = NAME_CACHE.getIfPresent(uniqueId);
        if (cached != null) {
            return cached;
        }
        try {
            String stringUniqueId = uniqueId.toString().replace("-", "");
            URL url = new URL("https://api.mojang.com/user/profiles/" + stringUniqueId + "/names");
            String name;
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream()))) {
                List<Name> history = GsonHolder.get().fromJson(reader, new TypeToken<List<Name>>() {
                }.getType());
                name = history.get(history.size() - 1).name;
            }
            if (name != null) {
                NAME_CACHE.put(uniqueId, name);
            }
            return name;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static class Name {
        private String name;
        private long changedToAt;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public long getChangedToAt() {
            return changedToAt;
        }

        public void setChangedToAt(long changedToAt) {
            this.changedToAt = changedToAt;
        }
    }
}
