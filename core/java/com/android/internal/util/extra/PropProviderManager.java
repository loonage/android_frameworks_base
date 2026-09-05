package com.android.internal.util.extra;

import android.content.Context;
import android.provider.Settings;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * Manager class for handling build-property providers.
 * @hide
 */
public final class PropProviderManager {
    private static final String TAG = "PropProviderManager";
    private static final String VERSION_PREFIX = "VERSION.";
    private static final Set<String> VERSION_FIELDS = Set.of(
        "SECURITY_PATCH",
        "DEVICE_INITIAL_SDK_INT",
        "RELEASE",
        "RELEASE_OR_CODENAME",
        "INCREMENTAL",
        "SDK_INT",
        "PREVIEW_SDK_INT",
        "BASE_OS",
        "CODENAME"
    );

    private PropProviderManager() {}

    public static IPropProvider getProvider(Context context) {
        return new DefaultPropProvider(context);
    }

    public static boolean isSpoofingEnabled(Context context) {
        return isSpoofEnabled(context);
    }

    private static String normalizeKey(String name) {
        if (name.startsWith(VERSION_PREFIX)) {
            return name;
        }
        if ("FIRST_API_LEVEL".equals(name)) {
            return VERSION_PREFIX + "DEVICE_INITIAL_SDK_INT";
        }
        if ("BUILD_ID".equals(name)) {
            return "ID";
        }
        if (VERSION_FIELDS.contains(name)) {
            return VERSION_PREFIX + name;
        }
        return name;
    }

    private static boolean isSpoofEnabled(Context ctx) {
        try {
            return Settings.Secure.getInt(ctx.getContentResolver(),
                    Settings.Secure.SPOOF_PROPS, 1) != 0;
        } catch (Exception e) {
            Log.e(TAG, "Failed to read " + Settings.Secure.SPOOF_PROPS, e);
            return true;
        }
    }

    private static class DefaultPropProvider implements IPropProvider {
        private final Map<String, String> props = new HashMap<>();

        private DefaultPropProvider(Context context) {
            if (context == null) {
                Log.e(TAG, "Null context, cannot load props");
                return;
            }

            if (!isSpoofEnabled(context)) {
                Log.i(TAG, "Props spoofing disabled, reporting real build");
                return;
            }

            loadFromJsonSetting(context);
        }

        private void loadFromJsonSetting(Context ctx) {
            try {
                String json = Settings.Secure.getString(
                        ctx.getContentResolver(), Settings.Secure.CERTIFIED_PROPS_DATA);
                if (json == null || json.trim().isEmpty()) {
                    Log.i(TAG, "No props profile set");
                    return;
                }

                JSONObject root = new JSONObject(json);
                for (Iterator<String> keys = root.keys(); keys.hasNext(); ) {
                    String name = keys.next();
                    Object value = root.opt(name);
                    if (value == null || value == JSONObject.NULL
                            || value instanceof JSONObject || value instanceof JSONArray) {
                        continue;
                    }
                    putProp(name, String.valueOf(value));
                }

                if (!hasProps()) {
                    Log.w(TAG, "Props profile contained no usable entries");
                    return;
                }

                Log.i(TAG, "Loaded " + props.size() + " props from JSON setting");
            } catch (JSONException e) {
                Log.e(TAG, "Malformed props JSON setting", e);
                props.clear();
            } catch (Exception e) {
                Log.e(TAG, "JSON props load failed", e);
                props.clear();
            }
        }

        private void putProp(String name, String value) {
            if (name == null || value == null) return;
            String field = normalizeKey(name.trim());
            String trimmed = value.trim();
            if (field.isEmpty() || trimmed.isEmpty()) return;
            props.put(field, trimmed);
        }

        @Override
        public boolean hasProps() {
            return !props.isEmpty();
        }

        @Override
        public Map<String, String> getProps() {
            return new HashMap<>(props);
        }
    }
}
