package davidgalindo.rhsexplore;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceGroup;

import davidgalindo.rhsexplore.preferences.NumberPickerPreference;

/**
 * Created by David on 5/8/2017.
 */

public class SettingsFragment extends PreferenceFragment implements SharedPreferences.OnSharedPreferenceChangeListener {
    @Override
    public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences);
        getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onResume(){
        super.onResume();
        //upon reentering this fragment we set the preference descriptions to what the are
        for (int i = 0; i < getPreferenceScreen().getPreferenceCount(); ++i) {
            Preference preference = getPreferenceScreen().getPreference(i);
            if (preference instanceof PreferenceGroup) {
                PreferenceGroup preferenceGroup = (PreferenceGroup) preference;
                for (int j = 0; j < preferenceGroup.getPreferenceCount(); ++j) {
                    Preference singlePref = preferenceGroup.getPreference(j);
                    updateMyPreference(singlePref, singlePref.getKey());
                }
            } else {
                updateMyPreference(preference, preference.getKey());
            }
        }
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sp, String key){
        updateMyPreference(findPreference(key),key);
    }

    private void updateMyPreference(Preference preference, String key){
        if(preference == null) return; //No preference, no work!
        if(preference instanceof ListPreference){//eg. A preference that lists out possible values
            //We'll just set the summary as its current value
            ListPreference lp = (ListPreference) preference;
            lp.setSummary(lp.getEntry());
            return;
        }//TODO: manage specific preferences here
        else if (preference instanceof NumberPickerPreference){
            NumberPickerPreference npp = (NumberPickerPreference) preference;
            npp.setSummary("Display the last " + npp.getValue() + " buildings viewed in your recents") ;
        }
    }
}
