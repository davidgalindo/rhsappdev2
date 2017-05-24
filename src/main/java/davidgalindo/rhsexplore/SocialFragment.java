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
import com.esri.core.map.CallbackListener;
import com.esri.core.map.Feature;
import com.esri.core.map.FeatureResult;
import com.esri.core.tasks.query.QueryParameters;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

import davidgalindo.rhsexplore.tools.SharedPreferenceManager;

/**
 * Created by David on 5/15/2017.
 * Populates both the Favorites list and the Recents list. Code reused for efficiency.
 */

public class SocialFragment extends Fragment {
    private ListView rootView;
    private JSONArray jsonArray;
    private SharedPreferenceManager sp;
    private GeodatabaseFeatureServiceTable ft;
    private ArrayList<House> houseList;
    private View view;
    private HouseAdapter itemsAdapter;




    public static SocialFragment fromJsonArray(String array){
        SocialFragment fragment = new SocialFragment();
        fragment.setJSONArray(array);
        return fragment;
    }

    private void setJSONArray(String array){
        try {
            jsonArray = new JSONArray(array);
        }catch (JSONException e){
            e.printStackTrace();
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState){
        view = inflater.inflate(R.layout.house_list, container, false);
        Log.i("views",""+(view==null));
        houseList = new ArrayList<House>();
        rootView = (ListView) view.findViewById(R.id.houseListRV);
        createArrayAdapter();
        ft = ((MainActivity)getActivity()).getFeatureTable();
        queryTableElements();
        return view;
    }


    public void createArrayAdapter() {
        itemsAdapter= new HouseAdapter(getActivity(), houseList);
        rootView.setAdapter(itemsAdapter);


    }

    private void queryTableElements(){
        try{
            Log.i("arrayElements",jsonArray.toString());
            //Note: display these backwards, eg. start at the end of the list (most recent item)

            //Inflate a view for each, asking the FeatureTable for information through the house ID
            ft = ((MainActivity)getActivity()).getFeatureTable();
            String whereExpression = "";
            whereExpression = extractIDs();
            fillList(whereExpression);
            showUI();

        }catch(JSONException | NullPointerException e){
            e.printStackTrace();
            Toast.makeText(getActivity().getApplicationContext(),"Nothing in this list.",Toast.LENGTH_LONG).show();
        }
    }
    private String extractIDs() throws JSONException{
        String string = "FID = ";
        for(int count=0;count<jsonArray.length();count++){
            JSONObject o = jsonArray.getJSONObject(count);
            o.get("id");
            string += o.get("id");
            if(count +1 < jsonArray.length()){
                string += " OR FID = ";
            }
        }
        return string;
    }

    private void fillList(String expression){
        QueryParameters q = new QueryParameters();
        q.setWhere(expression);
        if(ft.getStatus() != GeodatabaseFeatureServiceTable.Status.INITIALIZED){
            return;
        }

        ft.queryFeatures(q, new CallbackListener<FeatureResult>() {
            @Override
            public void onCallback(FeatureResult objects) {
                if(objects.featureCount() > 0){
                    Log.i("FeatureResult", "success");
                    int count = 0;
                    for(Object o: objects){
                        Feature feature = (Feature) o;
                        highlightHouseInfo(feature);
                        count++;
                        //Only load 10 houses, for now
                        if(count >100)
                            break;
                    }
                    Log.i("array",""+houseList.size());
                    itemsAdapter.notifyDataSetChanged();

                }else{
                    Toast.makeText(getActivity().getApplicationContext(),"No results found.",Toast.LENGTH_SHORT).show();
                }

            }

            @Override
            public void onError(Throwable throwable) {

            }
        });
    }

    private void showUI(){
        rootView.setVisibility(View.VISIBLE);
        view.findViewById(R.id.progressBar).setVisibility(View.GONE);

    }

    private void highlightHouseInfo(Feature feature) {
        //Let's select our match and extract some data from it.
        String name = (String) feature.getAttributeValue("NAME");
        String picURL = (String) feature.getAttributeValue("THUMB_URL");
        String yearBuilt = (String) feature.getAttributeValue("Year_Built");
        String houseURL = (String) feature.getAttributeValue("WEBSITE");
        houseList.add(new House(picURL, name, yearBuilt,houseURL));
    }

}
