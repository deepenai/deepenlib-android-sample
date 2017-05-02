package ai.deepen.android.deepenlibsample;

import android.content.Context;
import android.content.SharedPreferences;

public class Prefs {
    private Prefs() {
    }

    private static final String PREFS_FILE = "deepenlibprefs";
    private static SharedPreferences prefs;
    private static final String PREF_MODE = "mode";

    public static int getMode(Context context) {
        return getInt(context, PREF_MODE, 0);
    }
    public static void setMode(Context context, int value) {
        setInt(context, PREF_MODE, value);
    }

    private static void setInt(Context c, String key, int value) {
        if (prefs == null) {
            prefs = c.getSharedPreferences(PREFS_FILE, 0);
        }
        SharedPreferences.Editor e = prefs.edit();
        e.putInt(key, value);
        e.commit();
    }

    private static void setString(Context c, String key, String value) {
        if (prefs == null) {
            prefs = c.getSharedPreferences(PREFS_FILE, 0);
        }
        SharedPreferences.Editor e = prefs.edit();
        e.putString(key, value);
        e.commit();
    }

    private static int getInt(Context c, String key, int defaultValue) {
        if (prefs == null) {
            prefs = c.getSharedPreferences(PREFS_FILE, 0);
        }
        return prefs.getInt(key, defaultValue);
    }

    private static String getString(Context c, String key, String defaultValue) {
        if (prefs == null) {
            prefs = c.getSharedPreferences(PREFS_FILE, 0);
        }
        return prefs.getString(key, defaultValue);
    }
}
