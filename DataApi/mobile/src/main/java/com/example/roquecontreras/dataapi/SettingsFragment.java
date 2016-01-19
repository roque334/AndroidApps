package com.example.roquecontreras.dataapi;

import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;

import com.example.roquecontreras.common.MobileWearConstants;

/**
 * Created by roquecontreras on 17/11/15.
 */
public class SettingsFragment extends PreferenceFragment implements SharedPreferences.OnSharedPreferenceChangeListener {

    private SharedPreferences sharedPref;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        sharedPref = PreferenceManager.getDefaultSharedPreferences(getActivity());
        addPreferencesFromResource(R.xml.preferences);

        // loads and shows the current value in the settings screen
        for (int i = 0; i < getPreferenceScreen().getPreferenceCount(); i++) {
            pickPreferenceObject(getPreferenceScreen().getPreference(i));
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        getPreferenceScreen()
                .getSharedPreferences()
                .registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onPause() {
        super.onPause();
        getPreferenceScreen()
                .getSharedPreferences()
                .unregisterOnSharedPreferenceChangeListener(this);
    }

    /**
     * Goes through all the preferences widgets in the preferences layout.
     * @param p the Preference widget.
     */
    private void pickPreferenceObject(Preference p) {
        if (p instanceof PreferenceCategory) {
            PreferenceCategory cat = (PreferenceCategory) p;
            for (int i = 0; i < cat.getPreferenceCount(); i++) {
                pickPreferenceObject(cat.getPreference(i));
            }
        } else {
            updateSummary(p);
        }
    }

    /**
     * Updates all the checkboxes widget with their default or current value.
     * @param p the Preference widget.
     */
    private void updateSummary(Preference p) {
        String text;
        int intervalValue;
        Resources res = getResources();
        if (p instanceof NumberPickerPreference) {
            NumberPickerPreference editTextPref = (NumberPickerPreference) p;
            if (editTextPref.getKey().equalsIgnoreCase(MobileWearConstants.KEY_MEASUREMENTS_SAMPLE_INTERVAL)) {
                intervalValue = sharedPref.getInt(MobileWearConstants.KEY_MEASUREMENTS_SAMPLE_INTERVAL,res.getInteger(R.integer.measurement_sample_defaultValue));
                text = String.format(res.getString(R.string.measurement_sample_summary), intervalValue);
            }else{
                if(editTextPref.getKey().equalsIgnoreCase(MobileWearConstants.KEY_HANDHELD_WEAR_SYNC_INTERVAL)) {
                    intervalValue = sharedPref.getInt(MobileWearConstants.KEY_HANDHELD_WEAR_SYNC_INTERVAL, res.getInteger(R.integer.sync_interval_defaultValue));
                }else{
                    intervalValue = sharedPref.getInt(MobileWearConstants.KEY_HANDHELD_SERVER_SYNC_INTERVAL, res.getInteger(R.integer.sync_interval_defaultValue));
                }
                text = String.format(res.getString(R.string.sync_interval_summary), intervalValue);
            }
            editTextPref.setSummary(text);
        }
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        Preference pref = findPreference(key);
        updateSummary(pref);
    }

}
