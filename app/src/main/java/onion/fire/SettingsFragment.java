/*
 * Fire.onion
 *
 * http://play.google.com/store/apps/details?id=onion.fire
 * http://onionapps.github.io/Fire.onion/
 * http://github.com/onionApps/Fire.onion
 *
 * Author: http://github.com/onionApps - http://jkrnk73uid7p5thz.onion - bitcoin:1kGXfWx8PHZEVriCNkbP5hzD15HS4AyKf
 */

package onion.fire;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.view.View;

import java.io.File;

public class SettingsFragment extends PreferenceFragment {

    private static SettingsFragment instance;

    public static SettingsFragment getInstance() {
        return instance;
    }

    public SettingsFragment() {
        instance = this;
    }

    public void reset() {
        new AlertDialog.Builder(getActivity())
                .setTitle("Reset")
                .setMessage("Reset all settings to their default values?")
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Settings.getPrefs(getActivity()).edit().clear().commit();
                        Settings.getPrefs(getActivity());
                        restart();
                    }
                })
                .setNegativeButton("No", null)
                .show();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Settings.getPrefs(getActivity());
        addPreferencesFromResource(R.xml.prefs);



        findPreference("webrtc").setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(final Preference preference, Object newValue) {
                if (newValue == true) {
                    new AlertDialog.Builder(getActivity())
                            .setTitle("WARNING")
                            .setMessage("WebRTC may leak your IP if enabled!")
                            .setPositiveButton("Enable WebRTC", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    preference.getEditor().putBoolean("webrtc", true).commit();
                                    restart();

                                }
                            })
                            .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                }
                            })
                            .show();

                    return false;
                }
                return true;
            }
        });

        findPreference("about").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                startActivity(new Intent(getActivity(), AboutActivity.class));
                return true;
            }
        });

        findPreference("rate").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                BrowserActivity.getInstance().rate();
                return true;
            }
        });

        findPreference("share").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                BrowserActivity.getInstance().share();
                return true;
            }
        });

        findPreference("torwipe").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                new AlertDialog.Builder(getActivity())
                        .setTitle("Reset Tor?")
                        .setMessage("Wipe Tor directory and restart?")
                        .setNegativeButton("No", null)
                        .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                BrowserActivity.tor.killTorProcess();
                                BrowserActivity.getInstance().deleteFiles(new File(getActivity().getFilesDir(), "tordata"));
                                BrowserActivity.getInstance().restartApp();
                            }
                        })
                        .show();
                return true;
            }
        });



    }

    void restart() {

        BrowserActivity a = BrowserActivity.getInstance();
        a.skipcleanup = true;
        a.finish();

        getActivity().overridePendingTransition(0, 0);

        Intent intent = new Intent(getActivity(), BrowserActivity.class);
        intent.putExtra("init", false);
        intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);

        startActivity(intent);
        getActivity().overridePendingTransition(0, 0);

    }

    SharedPreferences.OnSharedPreferenceChangeListener changeListener = new SharedPreferences.OnSharedPreferenceChangeListener() {
        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {

            Prefs.applyPrefs(getActivity());

            update();

            if ("allowscreenshots".equals(key)) {
                restart();
            }

        }
    };

    void update() {
        final BrowserActivity a = BrowserActivity.getInstance();
        a.findViewById(R.id.settings_restart_bar).setVisibility(Settings.needsRestart(getActivity()) ? View.VISIBLE : View.GONE);
        a.findViewById(R.id.settings_restart_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                a.restartApp();
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        getPreferenceManager().getSharedPreferences().registerOnSharedPreferenceChangeListener(changeListener);
        update();
    }

    @Override
    public void onPause() {
        getPreferenceManager().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(changeListener);
        super.onPause();
    }


}
