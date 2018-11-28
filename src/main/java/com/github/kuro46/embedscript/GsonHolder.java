package com.github.kuro46.embedscript;

import com.google.gson.Gson;

import java.lang.ref.WeakReference;

/**
 * @author shirokuro
 */
public final class GsonHolder {
    private static WeakReference<Gson> gsonRef = new WeakReference<>(null);

    private GsonHolder() {
    }

    public static Gson get() {
        Gson gson = gsonRef.get();
        if (gson == null) {
            gsonRef = new WeakReference<>(gson = new Gson());
        }
        return gson;
    }
}
