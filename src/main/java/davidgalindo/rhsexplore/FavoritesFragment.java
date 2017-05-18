package davidgalindo.rhsexplore;

import android.app.Fragment;
import android.os.Bundle;
import android.preference.PreferenceManager;
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

public class FavoritesFragment extends Fragment {
    private ListView view;
    private JSONArray favoritesArray;
    private SharedPreferenceManager sp;
    private GeodatabaseFeatureServiceTable ft;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState){
        view = (ListView) inflater.inflate(R.layout.favorites_fragment,container,false);
        return view;
    }

    @Override
    public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        //Grab SharedPreferences
        sp = new SharedPreferenceManager(getActivity().getApplicationContext());
        //Grab our JSONArray for parsing
        try{
            favoritesArray = new JSONArray(sp.getJSONFavorites());

            Log.i("favorites",favoritesArray.toString());
            //Note: allow the user to rearrange objects in this list, and update the arrayList accordingly (a simple swap)

            //Inflate a view for each, asking the FeatureTable for information through the house ID
            ft = ((MainActivity)getActivity()).getFeatureTable();
            Log.i("favorites","Feature Table is null? " + (ft==null));
        }catch(JSONException e){
            e.printStackTrace();
            Toast.makeText(getActivity().getApplicationContext(),"No favorites.",Toast.LENGTH_LONG).show();
            //Do UI Work here for an empty list.
            //Toast.makeText(getActivity().getApplicationContext(),"Error while getting favorites, please try again later!",Toast.LENGTH_LONG).show();
        }
    }

}
