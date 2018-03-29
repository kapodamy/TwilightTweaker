package com.kapodamy.twilighttweaker;

/**
 * Created by kapodamy on 12/03/2018.
 */

import android.annotation.SuppressLint;
import android.app.UiModeManager;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.app.AppCompatDelegate;
import android.view.MenuItem;
import android.widget.Toast;

import java.util.HashSet;

public class TwiConfigActivity extends AppCompatActivity {
    static {
        // AppCompatDelegate.setCompatVectorFromResourcesEnabled(true);
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_AUTO);
    }
/*
    public int getVersion() {
        return Integer.parseInt("0",2);
    }*/

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(R.style.TwiTheme);
        super.onCreate(savedInstanceState);

/*        if (savedInstanceState == null) {
            int current = getVersion();
            if (current != XposedMain.VERSION) {
                AlertDialog alertDialog = new AlertDialog.Builder(TwiConfigActivity.this).create();
                alertDialog.setIcon(android.R.drawable.ic_dialog_info);
                alertDialog.setMessage(getString(current == 0 ? R.string.s_gancho : R.string.s_gancho2));
                alertDialog.setCanceledOnTouchOutside(true);
                alertDialog.setCancelable(true);
                alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, getString(android.R.string.ok),
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                        });
                alertDialog.show();
            }
        }*/

        SharedPreferences prefs = getSharedPreferences("ajustes", MODE_WORLD_READABLE);
        if (prefs.getAll().size() < 1) {
            prefs.edit()
                    .putBoolean("desactivar", false)
                    .putInt("modo", 0)
                    .putInt("minutos_inicio", 720)
                    .putInt("minutos_final", 72)
                    .putBoolean("usar_inicio", false)
                    .putBoolean("usar_final", false)
                    .putStringSet("lista", new HashSet<String>())
                    .putBoolean("admision", true)
                    .apply();
        }

        getFragmentManager().beginTransaction().replace(android.R.id.content, new MainPreferenceFragment()).commit();
    }

    public static class MainPreferenceFragment extends PreferenceFragment {

        private Preference.OnPreferenceChangeListener preferenceListener = new Preference.OnPreferenceChangeListener() {
            @SuppressLint("ApplySharedPref")
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                String key = preference.getKey();
                SharedPreferences.Editor editor = getActivity().getSharedPreferences(getString(R.string.prefs_store), MODE_WORLD_READABLE).edit();

                switch (key) {
                    case "desactivar":
                    case "admision":
                        editor.putBoolean(key, (boolean) newValue);
                        break;
                    case "modo":
                        editor.putInt(key, Integer.parseInt(((String) newValue)));
                        break;
                    default:
                        Toast.makeText(getActivity(), "ERROR: preferencia invalida: ".concat(key), Toast.LENGTH_LONG).show();
                        return true;
                }

                editor.apply();

                if (key.equals("modo") || key.equals("desactivar")) {
                    AppCompatActivity main = (AppCompatActivity)getActivity();
                    main.getDelegate().applyDayNight();//getActivity().recreate();
                }

                return true;
            }
        };

        @Override
        public void onCreate(final Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.pref_general);

            findPreference("modo").setOnPreferenceChangeListener(preferenceListener);
            findPreference("admision").setOnPreferenceChangeListener(preferenceListener);
            findPreference("desactivar").setOnPreferenceChangeListener(preferenceListener);

            findPreference("horarios").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                public boolean onPreferenceClick(Preference preference) {
                    lanzar(TwiHours.class);
                    return true;
                }
            });

            findPreference("select_app").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                public boolean onPreferenceClick(Preference preference) {
                    lanzar(TwiAppPick.class);
                    return true;
                }
            });

            findPreference("version").setTitle(getString(R.string.app_name).concat(" v").concat(BuildConfig.VERSION_NAME));
        }

        public void lanzar(Class<?> actividad) {
            getActivity().startActivity(new Intent(getActivity(), actividad));
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
        }
        return super.onOptionsItemSelected(item);
    }
}
