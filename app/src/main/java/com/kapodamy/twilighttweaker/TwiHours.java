package com.kapodamy.twilighttweaker;

/**
 * Created by kapodamy on 12/03/2018.
 */

import android.app.Dialog;
import android.app.TimePickerDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatDelegate;
import android.text.format.DateFormat;
import android.view.MenuItem;
import android.view.Window;
import android.widget.TimePicker;
import android.widget.Toast;

public class TwiHours extends AppCompatPreferenceActivity {
    private static final String TAG = TwiConfigActivity.class.getSimpleName();
    private static boolean IS12HS = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActionBar bar = getSupportActionBar();
        bar.setDisplayHomeAsUpEnabled(true);
        bar.setTitle(R.string.s_reloj);

        IS12HS = !DateFormat.is24HourFormat(getApplicationContext());

        getFragmentManager().beginTransaction().replace(android.R.id.content, new MainPreferenceFragment()).commit();
    }

    public static class MainPreferenceFragment extends PreferenceFragment {

        protected void getTime(TimeSpan time /*, TimeSpan reference*/, final Callback<TimeSpan> fn) {
            final TimePickerDialog picker = new TimePickerDialog(getActivity(), new TimePickerDialog.OnTimeSetListener() {
                @Override
                public void onTimeSet(TimePicker timePicker, int h, int m) {
                    fn.callback(new TimeSpan(h, m));
                }
            }, time.hour, time.minutes, !TwiHours.IS12HS);

            picker.setTitle(R.string.s_hora_establecer);
            picker.setCancelable(true);
            picker.setCanceledOnTouchOutside(true);

            picker.show();
        }

        protected TimeSpan getTime(SharedPreferences pref, String key) {
            float value = pref.getInt(key, 720);
            int hora = (int) (value / 60f);

            return new TimeSpan(hora, (int) value - (hora * 60));
        }

        protected String renderTime(TimeSpan ts) {
            StringBuilder str = new StringBuilder(8);
            boolean flag = ts.hour > 12;

            if (IS12HS && ts.hour > 12) {
                ts.hour -= 12;
            }
            str.append(ts.hour);

            str.append(':');

            if (ts.minutes < 10) {
                str.append('0');
            }
            str.append(ts.minutes);

            if (IS12HS) {
                str.append(flag ? " PM" : " AM");
            }

            return str.toString();
        }

        protected void setTime(TimeSpan ts, boolean inicio_o_final) {
            getActivity().getSharedPreferences(getString(R.string.prefs_store), MODE_WORLD_READABLE).edit().putInt(inicio_o_final ? "minutos_inicio" : "minutos_final", ts.getTotalMinutes()).apply();
        }

        protected void updateTime(Preference pref, TimeSpan ts, boolean inicio_or_final) {
            pref.setSummary(getString(R.string.ss_hora_definida, getString(inicio_or_final ? R.string.ss_jornada_incio : R.string.ss_jornada_finaliza), renderTime(ts)));
        }

        protected void checkTime(TimeSpan ts1, TimeSpan ts2) {
            if (ts1.getTotalMinutes() != ts2.getTotalMinutes()) {
                return;
            }
            Toast.makeText(getActivity(), R.string.s_hora_iguales, Toast.LENGTH_LONG).show();
        }

        protected void setOverride(boolean inicio_o_final, boolean value) {
            getActivity()
                    .getSharedPreferences(getString(R.string.prefs_store), MODE_WORLD_READABLE)
                    .edit()
                    .putBoolean(inicio_o_final ? "usar_inicio" : "usar_final", value)
                    .apply();
        }

        @Override
        public void onCreate(final Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.pref_horarios);

            SharedPreferences prefs = getActivity().getSharedPreferences(getString(R.string.prefs_store), MODE_WORLD_READABLE);

            final TimeSpan t_inicio = getTime(prefs, "minutos_inicio");
            final TimeSpan t_final = getTime(prefs, "minutos_final");

            final Preference sumario_inicio = findPreference("establecer_inicio");
            final Preference sumario_final = findPreference("establecer_final");

            sumario_inicio.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    getTime(t_inicio, new Callback<TimeSpan>() {
                        @Override
                        public void callback(TimeSpan result) {
                            t_inicio.setTimeSpan(result);
                            updateTime(sumario_inicio, t_inicio, true);
                            setTime(t_inicio, true);
                            if (sumario_final.isEnabled()) {
                                checkTime(t_inicio, t_final);
                            }
                        }
                    });
                    return true;
                }
            });

            sumario_final.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    getTime(t_final, new Callback<TimeSpan>() {
                        @Override
                        public void callback(TimeSpan result) {
                            t_final.setTimeSpan(result);
                            updateTime(sumario_final, t_final, false);
                            setTime(t_final, false);
                            if (sumario_inicio.isEnabled()) {
                                checkTime(t_inicio, t_final);
                            }
                        }
                    });
                    return true;
                }
            });

            CheckBoxPreference usar_inicio = (CheckBoxPreference) findPreference("usar_inicio");
            CheckBoxPreference usar_final = (CheckBoxPreference) findPreference("usar_final");

            sumario_inicio.setEnabled(usar_inicio.isChecked());
            sumario_final.setEnabled(usar_final.isChecked());


            updateTime(sumario_inicio, t_inicio, true);
            updateTime(sumario_final, t_final, false);


            if (usar_inicio.isChecked() && usar_final.isChecked()) {
                checkTime(t_inicio, t_final);
            }

            usar_inicio.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    sumario_inicio.setEnabled((boolean) newValue);
                    setOverride(true, (boolean) newValue);
                    return true;
                }
            });

            usar_final.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    sumario_final.setEnabled((boolean) newValue);
                    setOverride(false, (boolean) newValue);
                    return true;
                }
            });
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
