package cf.vojtechh.apkmirror;

import android.app.Application;
import android.content.SharedPreferences;

import androidx.appcompat.app.AppCompatDelegate;
import androidx.preference.PreferenceManager;

public class App extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        applyThemeMode(prefs.getString("theme_mode", "system"));
    }

    /** "system" (default), "light" or "dark": the app and the site follow it. */
    public static void applyThemeMode(String mode) {
        int nightMode;
        if ("dark".equals(mode)) {
            nightMode = AppCompatDelegate.MODE_NIGHT_YES;
        } else if ("light".equals(mode)) {
            nightMode = AppCompatDelegate.MODE_NIGHT_NO;
        } else {
            nightMode = AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM;
        }
        AppCompatDelegate.setDefaultNightMode(nightMode);
    }
}
