package onion.fire;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import java.util.Map;

public class Settings {

    static String TAG = "Settings";

    static Map<String, ?> initialValues;

    public synchronized static SharedPreferences getPrefs(Context c) {

        c = c.getApplicationContext();
        SharedPreferences p = PreferenceManager.getDefaultSharedPreferences(c);
        PreferenceManager.setDefaultValues(c, R.xml.prefs, false);

        if (initialValues == null) {
            initialValues = p.getAll();
        }

        return p;

    }

    public static boolean needsRestart(Context c) {

        Map<String, ?> currentValues = getPrefs(c).getAll();

        for (String k : new String[]{"entrynodes", "exitnodes", "excludenodes", "strictnodes", "torcustom",}) {
            Object a = initialValues.get(k);
            Object b = currentValues.get(k);
            if (a == null) a = "";
            if (b == null) b = "";
            a = a.toString();
            b = b.toString();
            Log.i(TAG, "needsRestart: " + a + " " + b);
            if (!a.equals(b)) {
                return true;
            }
        }

        return false;

    }

}
