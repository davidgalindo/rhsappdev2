package davidgalindo.rhsexplore;

import android.app.Fragment;
import android.os.Bundle;
import android.preference.PreferenceFragment;

/**
 * Created by David on 5/8/2017.
 */

public class SettingsFragment extends PreferenceFragment {
    @Override
    public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences);
    }
}
