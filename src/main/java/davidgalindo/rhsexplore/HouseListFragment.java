package davidgalindo.rhsexplore;

import android.app.Fragment;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.ListView;
import android.widget.Toast;

import com.esri.core.geodatabase.GeodatabaseFeatureServiceTable;
import com.esri.core.map.CallbackListener;
import com.esri.core.map.Feature;
import com.esri.core.map.FeatureResult;
import com.esri.core.tasks.query.QueryParameters;

import java.util.ArrayList;

import davidgalindo.rhsexplore.tools.DownloadingView;


/**
 * Created by Sean on 5/13/2017.
 */
public class HouseListFragment extends Fragment {
    private GeodatabaseFeatureServiceTable ft;
    private View view;
    ListView rootView;
    HouseAdapter itemsAdapter;

    ArrayList<House> houseList;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.house_list, container, false);
        Log.i("views",""+(view==null));
        houseList = new ArrayList<>();
        rootView = (ListView) view.findViewById(R.id.houseListRV);
        createArrayAdapter();
        ft = ((MainActivity)getActivity()).getFeatureTable();
        queryTableElements();
        return view;
    }


    public void createArrayAdapter() {
        itemsAdapter= new HouseAdapter(getActivity(), houseList);
        rootView.setAdapter(itemsAdapter);
        rootView.setVisibility(View.VISIBLE);
        view.findViewById(R.id.progressBar).setVisibility(View.GONE);
    }

    public void queryTableElements() {
        QueryParameters q = new QueryParameters();
        q.setWhere("State LIKE '" + "CA" +"'");

        if(ft.getStatus() != GeodatabaseFeatureServiceTable.Status.INITIALIZED){
            Toast.makeText(getActivity().getApplicationContext(),"Error loading table, please try again later.",Toast.LENGTH_LONG).show();
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
                    Log.i("House list", Integer.toString(houseList.size()));
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

    private void highlightHouseInfo(Feature feature) {
        //Let's select our match and extract some data from it.
        String name = (String) feature.getAttributeValue("NAME");
        String picURL = (String) feature.getAttributeValue("THUMB_URL");
        String yearBuilt = (String) feature.getAttributeValue("Year_Built");
        String houseURL = (String) feature.getAttributeValue("WEBSITE");
        houseList.add(new House(picURL, name, yearBuilt,houseURL));
    }
}