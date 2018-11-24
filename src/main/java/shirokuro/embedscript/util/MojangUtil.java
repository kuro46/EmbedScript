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
    private static final Cache<String, UUID> UUID_CACHE = CacheBuilder.newBuilder()
        .expireAfterAccess(1, TimeUnit.HOURS)
        .build();

    private MojangUtil() {

    }

    public static String getName(UUID uniqueId) {
        String cached = NAME_CACHE.getIfPresent(uniqueId);
        if (cached != null) {
            return cached;
        }

        String stringUniqueId = uniqueId.toString().replace("-", "");
        String urlString = "https://api.mojang.com/user/profiles/" + stringUniqueId + "/names";
        String name = null;

        try (JsonReader reader = newReader(urlString)) {
            reader.beginArray();
            while (reader.hasNext()) {
                name = readName(reader);
            }
            reader.endArray();
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (name != null) {
            NAME_CACHE.put(uniqueId, name);
            return name;
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

    public static UUID getUUID(String name) {
        UUID cache = UUID_CACHE.getIfPresent(name);
        if (cache != null) {
            return cache;
        }

        String stringUrl = "https://api.mojang.com/users/profiles/minecraft/" + name;
        String stringUniqueId = null;

        try (JsonReader reader = newReader(stringUrl)) {
            reader.beginObject();
            while (reader.hasNext()) {
                if (reader.nextName().equals("id")) {
                    stringUniqueId = reader.nextString();
                } else {
                    reader.skipValue();
                }
            }
            reader.endObject();
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (stringUniqueId != null) {
            UUID uniqueId = toUUID(stringUniqueId);
            UUID_CACHE.put(name, uniqueId);
            return uniqueId;
        }
        return null;
    }

    private static JsonReader newReader(String urlString) throws IOException {
        URL url = new URL(urlString);
        return new JsonReader(new BufferedReader(new InputStreamReader(url.openStream())));
    }

    private static UUID toUUID(String shortUUID) {
        StringBuilder sb = new StringBuilder(36);
        sb.append(shortUUID);
        sb.insert(20, '-');
        sb.insert(16, '-');
        sb.insert(12, '-');
        sb.insert(8, '-');

        return UUID.fromString(sb.toString());
    }
}
