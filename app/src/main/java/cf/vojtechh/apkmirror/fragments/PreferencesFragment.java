package cf.vojtechh.apkmirror.fragments;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDelegate;
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
        noIconSpace(getPreferenceScreen());

        Preference github = findPreference("github");
        Preference libs = findPreference("libs");
        ListPreference theme = findPreference("theme_mode");
        ListPreference language = findPreference("language");

        if (language != null) {
            // show the language that is really in use (the system may have changed it from its own settings)
            androidx.core.os.LocaleListCompat current = AppCompatDelegate.getApplicationLocales();
            language.setValue(current.isEmpty() ? "system" : matchTag(current.toLanguageTags(), language));
            language.setOnPreferenceChangeListener((pref, newValue) -> {
                String tag = String.valueOf(newValue);
                AppCompatDelegate.setApplicationLocales("system".equals(tag)
                        ? androidx.core.os.LocaleListCompat.getEmptyLocaleList()
                        : androidx.core.os.LocaleListCompat.forLanguageTags(tag));
                return true;
            });
        }

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

                LicensesDialog.Builder builder = new LicensesDialog.Builder(requireActivity())
                        .setNotices(notices)
                        .setTitle(getString(R.string.libraries));
                boolean night = (getResources().getConfiguration().uiMode
                        & android.content.res.Configuration.UI_MODE_NIGHT_MASK)
                        == android.content.res.Configuration.UI_MODE_NIGHT_YES;
                if (night) {
                    // dark page inside the dialog (the library draws a white one by default)
                    builder.setNoticesCssStyle("body{padding:0;margin:0;background-color:" + hex(R.color.dialog_bg) + ";color:#eeeeee;}"
                            + "a{color:" + hex(R.color.colorPrimary) + ";}"
                            + "pre{background-color:" + hex(R.color.card_bg_3) + ";color:#eeeeee;padding:1em;white-space:pre-wrap;}");
                }
                builder.build().show();
                return true;
            });
        }
    }

    private String hex(int colorRes) {
        return String.format("#%06X", androidx.core.content.ContextCompat.getColor(requireContext(), colorRes) & 0xFFFFFF);
    }

    /** No empty column on the left: the preferences have no icons. */
    private static void noIconSpace(androidx.preference.PreferenceGroup group) {
        group.setIconSpaceReserved(false);
        for (int i = 0; i < group.getPreferenceCount(); i++) {
            Preference p = group.getPreference(i);
            p.setIconSpaceReserved(false);
            if (p instanceof androidx.preference.PreferenceGroup) noIconSpace((androidx.preference.PreferenceGroup) p);
        }
    }

    /** The saved tag ("it-IT", "pt-BR"...) -> the closest value of the list ("it", "pt-BR"). */
    private static String matchTag(String tags, ListPreference list) {
        String first = tags.split(",")[0];
        CharSequence[] values = list.getEntryValues();
        for (CharSequence v : values) if (v.toString().equalsIgnoreCase(first)) return v.toString();
        String lang = first.split("-")[0];
        for (CharSequence v : values) if (v.toString().equalsIgnoreCase(lang)) return v.toString();
        return "system";
    }
}
