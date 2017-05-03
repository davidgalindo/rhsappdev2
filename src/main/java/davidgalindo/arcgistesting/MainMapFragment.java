package davidgalindo.arcgistesting;

import android.app.Fragment;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;

/**
import com.esri.arcgisruntime.concurrent.ListenableFuture;
import com.esri.arcgisruntime.data.FeatureQueryResult;
import com.esri.arcgisruntime.data.FeatureTable;
import com.esri.arcgisruntime.data.Field;
import com.esri.arcgisruntime.data.QueryParameters;
import com.esri.arcgisruntime.data.ServiceFeatureTable;
import com.esri.arcgisruntime.geometry.Envelope;
import com.esri.arcgisruntime.geometry.Point;
import com.esri.arcgisruntime.geometry.SpatialReference;
import com.esri.arcgisruntime.layers.ArcGISTiledLayer;
import com.esri.arcgisruntime.layers.FeatureLayer;
import com.esri.arcgisruntime.mapping.ArcGISMap;
import com.esri.arcgisruntime.mapping.Basemap;
import com.esri.arcgisruntime.mapping.view.DefaultMapViewOnTouchListener;
import com.esri.arcgisruntime.mapping.view.MapView;
import com.esri.arcgisruntime.portal.Portal;
import com.esri.arcgisruntime.portal.PortalItem;
 **/
import com.esri.android.map.FeatureLayer;
import com.esri.android.map.MapView;
import com.esri.android.map.event.OnSingleTapListener;
import com.esri.core.geodatabase.GeodatabaseFeatureServiceTable;
import com.esri.core.geometry.Envelope;
import com.esri.core.geometry.Point;
import com.esri.core.map.CallbackListener;
import com.esri.core.map.Feature;
import com.esri.core.table.FeatureTable;
import com.esri.core.tasks.query.QueryParameters;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Created by David on 4/26/2017.
 * Fragment that contains all the functionality of the ArcGIS Map.
 */

public class MainMapFragment extends Fragment {
    private MapView mapView;
    private GeodatabaseFeatureServiceTable ft;
    private FeatureLayer mFeatureLayer;
    private View view;
    private final String PORTAL_URL = "http://services7.arcgis.com/bRDoEnap5EYRc1GS/arcgis/rest/services/HousesTL/MapServer";

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        view = inflater.inflate(R.layout.arcgis_map_view, container, false);
        return view;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState){
        super.onActivityCreated(savedInstanceState);
        mapView = (MapView) view.findViewById(R.id.mapView);
        mapView.enableWrapAround(true);
        //Feature Table to create the FeatureLayer
        ft = new GeodatabaseFeatureServiceTable("http://services7.arcgis.com/bRDoEnap5EYRc1GS/ArcGIS/rest/services/Houses/FeatureServer",0);
        //Now we gotta initialize the Table to make sure our data is accessed correctly.
        ft.initialize(new CallbackListener<GeodatabaseFeatureServiceTable.Status>() {
            @Override
            public void onCallback(GeodatabaseFeatureServiceTable.Status status) {
                if(status == GeodatabaseFeatureServiceTable.Status.INITIALIZED){ //eg. A success
                    //Then assign the table to a FeatureLayer and then place it onto the map
                    mFeatureLayer = new FeatureLayer(ft);
                    mapView.addLayer(mFeatureLayer);
                }
            }
            @Override
            public void onError(Throwable throwable) {
                Toast.makeText(getActivity().getApplicationContext(),"Error while loading data.",Toast.LENGTH_LONG).show();
            }
        });

        mapView.setOnSingleTapListener(new OnSingleTapListener() {
            @Override
            public void onSingleTap(float x, float y) {
                int tolerance = 10; //To account for slight error
                //ie. If there's a point there
                if(mFeatureLayer.getFeatureIDs(x,y,tolerance).length !=0){

                    //Self explanitory. React based on where the user tapped.
                    mFeatureLayer.clearSelection(); //Clear any previous selection
                    Point userSelection = mapView.toMapPoint(x,y);
                    //Essentially any poitns that fall within our selection will have their ID in this array
                    long[] ids = mFeatureLayer.getFeatureIDs(x,y,tolerance);
                    if(ids.length >1){//If we have more than one match, let's just say, for now, we found more than
                        //One match and call it a day
                        Toast.makeText(getActivity().getApplicationContext(),ids.length + " features found.",Toast.LENGTH_LONG).show();
                        return;
                    }
                    //If not, let's give specific information about the selection.
                    //Now we highlight the first feature
                    mFeatureLayer.selectFeature(ids[0]);
                    //Let's select our match and extract some data from it.
                    Feature feature = mFeatureLayer.getFeature(ids[0]);
                    String name = (String) feature.getAttributeValue("NAME");
                    String address = (String) feature.getAttributeValue("Street");
                    String cityZip = feature.getAttributeValue("City") + ", " + feature.getAttributeValue("State");
                    Toast.makeText(getActivity().getApplicationContext(),name + "\n"+address + "\n"
                            + cityZip,Toast.LENGTH_LONG).show();

                }

            }
        });

        //final FeatureLayer fl = new FeatureLayer(ft);

        //Instantiate the map, add our FeatureLayer, and then display it into the view


    }

    @Override
    public void onPause(){
        super.onPause();
        mapView.pause();
    }

    @Override
    public void onResume(){
        super.onResume();
        mapView.unpause();
    }
}
