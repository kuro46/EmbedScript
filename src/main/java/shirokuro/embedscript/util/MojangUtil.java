package shirokuro.embedscript.util;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.gson.stream.JsonReader;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
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
            String name = null;
            try (JsonReader reader = new JsonReader(new BufferedReader(new InputStreamReader(url.openStream())))) {
                reader.beginArray();
                while (reader.hasNext()) {
                    name = readName(reader);
                }
                reader.endArray();
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

    private static String readName(JsonReader reader) throws IOException {
        String name = null;

        reader.beginObject();
        while (reader.hasNext()) {
            if (reader.nextName().equals("name")) {
                name = reader.nextString();
            } else {
                reader.skipValue();
            }
        }
        reader.endObject();

        return name;
    }
}
