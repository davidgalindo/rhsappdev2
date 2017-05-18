package davidgalindo.rhsexplore;

import android.app.Fragment;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.Toast;

import com.esri.core.geodatabase.GeodatabaseFeatureServiceTable;

import org.json.JSONArray;
import org.json.JSONException;

import davidgalindo.rhsexplore.tools.SharedPreferenceManager;

/**
 * Created by David on 5/15/2017.
 */

public class RecentsFragment extends Fragment {
    private ListView view;
    private JSONArray recentsArray;
    private SharedPreferenceManager sp;
    private GeodatabaseFeatureServiceTable ft;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState){
        view = (ListView) inflater.inflate(R.layout.recents_fragment,container,false);
        return view;
    }

    @Override
    public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        //Grab SharedPreferences
        sp = new SharedPreferenceManager(getActivity().getApplicationContext());
        //Grab our JSONArray for parsing
        try{
            recentsArray = new JSONArray(sp.getJSONRecents());
            Log.i("recents",recentsArray.toString());
            //Note: display these backwards, eg. start at the end of the list (most recent item)

            //Inflate a view for each, asking the FeatureTable for information through the house ID
            ft = ((MainActivity)getActivity()).getFeatureTable();
            Log.i("recents","Feature Table is null? " + (ft==null));
        }catch(JSONException e){
            e.printStackTrace();
            Toast.makeText(getActivity().getApplicationContext(),"No recents.",Toast.LENGTH_LONG).show();
            //Do UI work here, or make another method for it

            //Toast.makeText(getActivity().getApplicationContext(),"Error while getting recents, please try again later!",Toast.LENGTH_LONG).show();
        }
    }

}
