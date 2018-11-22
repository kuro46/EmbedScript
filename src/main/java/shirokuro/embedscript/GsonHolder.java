package shirokuro.embedscript;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.lang.ref.WeakReference;

/**
 * @author shirokuro
 */
public final class GsonHolder {
    @Deprecated
    public static final Gson GSON = new GsonBuilder()
        .enableComplexMapKeySerialization()
        .create();
    private static WeakReference<Gson> gsonRef = new WeakReference<>(null);

    private GsonHolder() {
    }

    public static Gson get() {
        Gson gson = gsonRef.get();
        if (gson == null) {
            gsonRef = new WeakReference<>(gson = new GsonBuilder()
                .enableComplexMapKeySerialization()
                .create());
        }
        return gson;
    }
}
