package cf.vojtechh.apkmirror.fragments;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import cf.vojtechh.apkmirror.App;
import cf.vojtechh.apkmirror.R;
import de.psdev.licensesdialog.LicensesDialog;
import de.psdev.licensesdialog.licenses.ApacheSoftwareLicense20;
import de.psdev.licensesdialog.licenses.GnuGeneralPublicLicense20;
import de.psdev.licensesdialog.model.Notice;
import de.psdev.licensesdialog.model.Notices;

public class PreferencesFragment extends PreferenceFragmentCompat {

    @Override
    public void onCreatePreferences(@Nullable Bundle savedInstanceState, @Nullable String rootKey) {
        setPreferencesFromResource(R.xml.preferences, rootKey);

        Preference github = findPreference("github");
        Preference libs = findPreference("libs");
        Preference xda = findPreference("xda");
        ListPreference theme = findPreference("theme_mode");

        if (theme != null) {
            theme.setOnPreferenceChangeListener((pref, newValue) -> {
                App.applyThemeMode(String.valueOf(newValue));
                return true;
            });
        }

        if (github != null) {
            github.setSummary(R.string.github_summ_fork);
            github.setOnPreferenceClickListener(pref -> {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/tugaia56/APKMirror")));
                return true;
            });
        }

        if (libs != null) {
            libs.setOnPreferenceClickListener(pref -> {
                final Notices notices = new Notices();
                notices.addNotice(new Notice("APKMirror (original app)", "https://github.com/vojta-horanek/APKMirror",
                        "Copyright (c) Vojtěch Hořánek", new GnuGeneralPublicLicense20()));
                notices.addNotice(new Notice("AndroidX, WebKit and Material Components", "https://developer.android.com/jetpack",
                        "Copyright (c) The Android Open Source Project / Google", new ApacheSoftwareLicense20()));
                notices.addNotice(new Notice("LicensesDialog", "http://psdev.de",
                        "Copyright 2013 Philip Schiffer <admin@psdev.de>", new ApacheSoftwareLicense20()));

                new LicensesDialog.Builder(requireActivity())
                        .setNotices(notices)
                        .setTitle(getString(R.string.libraries))
                        .build()
                        .show();
                return true;
            });
        }

        if (xda != null) {
            xda.setOnPreferenceClickListener(pref -> {
                startActivity(new Intent(Intent.ACTION_VIEW,
                        Uri.parse("https://forum.xda-developers.com/android/apps-games/apkmirror-web-app-t3450564")));
                return true;
            });
        }
    }
}
