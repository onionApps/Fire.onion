/*
 * Fire.onion
 *
 * https://play.google.com/store/apps/details?id=onion.fire
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
import android.preference.PreferenceActivity;
import android.view.Menu;
import android.view.MenuItem;

public class SettingsActivity2 extends PreferenceActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Settings.getPrefs(this);
        //setPreferenceScreen();
        addPreferencesFromResource(R.xml.prefs);

        findPreference("webrtc").setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(final Preference preference, Object newValue) {

                if (newValue == true) {
                    new AlertDialog.Builder(SettingsActivity2.this)
                            .setTitle("WARNING")
                            .setMessage("WebRTC may leak your IP if enabled!")
                            .setPositiveButton("Enable WebRTC", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    SharedPreferences.Editor editor = preference.getEditor();

                                    //preference.notifyDependencyChange(false);

                                    Intent intent = getIntent();
                                    finish();

                                    editor.putBoolean("webrtc", true).commit();

                                    startActivity(intent);

                                    overridePendingTransition(0, 0);

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
                startActivity(new Intent(SettingsActivity2.this, AboutActivity.class));
                return true;
            }
        });

    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_prefs, menu);
        return true;
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        int id = item.getItemId();

        if (id == R.id.action_reset) {

            new AlertDialog.Builder(this)
                    .setTitle("Reset")
                    .setMessage("Reset all settings to their default values?")
                    .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            Settings.getPrefs(SettingsActivity2.this).edit().clear().commit();
                            Settings.getPrefs(SettingsActivity2.this);

                            Intent intent = getIntent();
                            finish();
                            startActivity(intent);
                            overridePendingTransition(0, 0);
                        }
                    })
                    .setNegativeButton("No", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {

                        }
                    })
                    .show();

            return true;
        }

        return super.onOptionsItemSelected(item);
    }

}
