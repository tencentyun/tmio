/**
 * Copyright (c) 2021 Tencent
 */

package com.tencent.xbright.tmio_demo.config;

import android.content.Context;
import android.content.SharedPreferences;

public class ConfigSharePref {
    private static final String TAG="SharePreferences";
    private static String configName = "testContextSp";
    private static SharedPreferences mContextSp;

    public static void initAppConfig(Context context) {
        mContextSp = context.getSharedPreferences(configName, Context.MODE_PRIVATE);
    }

    public static String getConfigString(String key, String defaultValue) {
        return mContextSp.getString(key, defaultValue);
    }

    public static void updateConfig(String key, String config) {
        SharedPreferences.Editor editor = mContextSp.edit();
        editor.putString(key, config);
        editor.apply();
    }
}
