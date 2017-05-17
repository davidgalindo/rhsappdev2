package davidgalindo.rhsexplore;

import android.Manifest;
import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.support.annotation.MainThread;
import android.support.annotation.RequiresPermission;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.esri.android.map.FeatureLayer;
import com.esri.android.map.GraphicsLayer;
import com.esri.android.map.Layer;
import com.esri.android.map.MapView;
import com.esri.android.map.event.OnSingleTapListener;
import com.esri.android.map.event.OnStatusChangedListener;
import com.esri.core.geodatabase.GeodatabaseFeatureServiceTable;
import com.esri.core.geometry.CoordinateConversion;
import com.esri.core.geometry.Point;
import com.esri.core.map.CallbackListener;
import com.esri.core.map.Feature;
import com.esri.core.map.FeatureResult;
import com.esri.core.map.Graphic;
import com.esri.core.symbol.SimpleMarkerSymbol;
import com.esri.core.tasks.query.QueryParameters;


/**
 * Created by David on 4/26/2017.
 * Fragment that contains all the functionality of the ArcGIS Map.
 */

public class MainMapFragment extends Fragment {
    private MapView mapView;
    public static final int PERMISSION_ACCESS_FINE_LOCATION = 1;
    private CoordinatorLayout view;
    private GeodatabaseFeatureServiceTable ft;
    private FeatureLayer mFeatureLayer;
    private Snackbar snackBar;
    //Layer used for navigation drawing and current location mapping
    private GraphicsLayer locationLayer;
    private MapLocationListener locationListener;
    private Location location;
    Point lastCenter;
    private final Point DOWNTOWN_REDLANDS = new Point(34.055569,-117.182538);
    private Intent houseIntent; //responsible for sending us to a new house

    @Override
    public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        //Set up location listener for later
        lastCenter = null;
        locationListener = new MapLocationListener();
    }
    private void addLayers(){
        mapView.addLayer(mFeatureLayer = new FeatureLayer(ft));
        mapView.addLayer(locationLayer = ((MainActivity)getActivity()).getGraphicsLayer());
    }

    @Override
    public CoordinatorLayout onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        ft = ((MainActivity)getActivity()).getFeatureTable();
        // Inflate the layout for this fragment
        view = (CoordinatorLayout) inflater.inflate(R.layout.arcgis_map_view, container, false);
        mapView = (MapView) view.findViewById(R.id.mapView);
        mapView.enableWrapAround(true);
        //Reacts when a user tries to select a point on the map.
        mapView.setOnSingleTapListener(new OnMapItemClickListener());
        //Set Precise Location Button Listener
        FloatingActionButton preciseLocationBtn = (FloatingActionButton) view.findViewById(R.id.fab);
        preciseLocationBtn.setOnClickListener(new LocationFabListener());
        //Set up references
        snackBar = Snackbar.make(view,"blank",0);
        mapView.setOnStatusChangedListener(new OnStatusChangedListener() {
            @Override
            public void onStatusChanged(Object o, STATUS status) {
                if(status == STATUS.LAYER_LOADED){
                    //Make sure we're only setting the center once
                    if(((Layer)o).getID() == mFeatureLayer.getID()){
                        if(ft != null) {
                            setMapCenter();
                        }
                    }
                }
                else if (status == STATUS.INITIALIZED){
                    mapView.setEsriLogoVisible(true);
                }
            }
        });
        return view;
    }

    @Override public void onPause() {
        super.onPause();
        ((MainActivity)getActivity()).setPoint(mapView.getCenter());
        Log.i("lastCenter onPause",""+ (lastCenter==null));
        mapView.removeLayer(locationLayer);
        mapView.removeLayer(mFeatureLayer);
        mapView.pause();
    }
    @Override public void onResume() {super.onResume();
        mapView.unpause();
        if(ft!=null)
            addLayers();
        lastCenter = ((MainActivity)getActivity()).getPoint();
    }

    private void setMapCenter(){
        //ft = ((MainActivity)getActivity()).getFeatureTable();
        final SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(getActivity().getApplicationContext());
        String startingLocation = sp.getString("starting_point", "None!");
        String startURL = sp.getString("startURL","");
        Log.i("lastCenter",""+ (lastCenter==null));
        if(lastCenter != null){
            Log.i("lastCenter","Initialized to last Center");
            mapView.centerAt(lastCenter, false);
        }
        else if (!startURL.equals("")){
            Log.i("query","Search for " + startURL);
            QueryParameters q = new QueryParameters();
            q.setWhere("WEBSITE LIKE '" + startURL +"'");

            ft.queryFeatures(q, new CallbackListener<FeatureResult>() {
                @Override
                public void onCallback(FeatureResult objects) {
                    if(objects.featureCount() > 0){
                        Log.i("query","Object found " );
                        for(Object o: objects){
                            Feature feature = (Feature) o;
                            highlightHouseInfo(feature.getId());
                        }
                        sp.edit().putString("startURL","").apply();
                    }else{
                        Toast.makeText(getActivity().getApplicationContext(),"No results found.",Toast.LENGTH_LONG).show();

                    }
                }

                @Override
                public void onError(Throwable throwable) {
                    Log.i("query","Some error occurred. ");
                }
            });
        }
        else if (startingLocation.equals("currentlocation")) {
            grabCurrentUserLocation();
            Log.i("Shared Preferences", "Initialized to Current Location");
        } else {
            mapView.centerAndZoom(DOWNTOWN_REDLANDS.getX(), DOWNTOWN_REDLANDS.getY(), 16);
            Log.i("Shared Preferences", "Initialized to Downtown");
        }
    }


    private void highlightHouseInfo(long id){
        //Let's select our match and extract some data from it.
        Log.i("query","Highlighting house " + id );
        Feature feature = mFeatureLayer.getFeature(id);
        mFeatureLayer.clearSelection();
        //Gather the intent data in the background, and once it's ready, show it!
        new IntentGeneratingAsyncTask(feature).execute();

    }

    //I made this asynctask to reduce the load on the main thread
    private class IntentGeneratingAsyncTask extends AsyncTask<Void,Void,Integer> {
        String houseName;
        String houseAddress;
        String houseBuiltAwarded;
        String imgUrl;
        String websiteURL;
        long houseId;
        Feature feature;
        public IntentGeneratingAsyncTask(Feature feature){
            this.feature = feature;
        }

        @Override
        public Integer doInBackground(Void... params){
            String name = (String) feature.getAttributeValue("NAME");
            String address = (String) feature.getAttributeValue("Street");
            String picURL = (String) feature.getAttributeValue("PIC_URL");
            String builtAwarded ="Built " + feature.getAttributeValue("Year_Built")+ ", Awarded " + feature.getAttributeValue("Year_Awarded");
            String websiteURL = (String) feature.getAttributeValue("WEBSITE");
            houseAddress = address;
            houseName = name;
            houseBuiltAwarded = builtAwarded;
            imgUrl = picURL;
            this.websiteURL = websiteURL;
            houseId = feature.getId();
            //Set up an intent to pass on information to our HouseInfoActivity
            houseIntent = new Intent(getActivity().getApplicationContext(),HouseInfoActivity.class);
            houseIntent.putExtra("houseName",houseName);
            houseIntent.putExtra("houseAddress",houseAddress);
            houseIntent.putExtra("houseBuiltAwarded",houseBuiltAwarded);
            houseIntent.putExtra("houseImgUrl",imgUrl);
            houseIntent.putExtra("websiteURL",websiteURL);
            houseIntent.putExtra("houseId",houseId);

            return 1;
        }

        @Override
        public void onPostExecute(Integer i){
            Point p = (Point) feature.getGeometry();
            mapView.centerAt(p,true);
            mFeatureLayer.selectFeature(houseId);
            String coords = CoordinateConversion.pointToDegreesMinutesSeconds(mapView.getCenter(),mapView.getSpatialReference(),6);
            houseIntent.putExtra("houseCoords",coords);
            HouseInfoOnClickListener onClickListener = new HouseInfoOnClickListener(houseIntent);
            //Let's use a SnackBar to present the name in a less gaudy format
            snackBar = Snackbar.make(view,houseName,Snackbar.LENGTH_INDEFINITE);
            snackBar.setAction("INFO",onClickListener).show();
        }
    }

    /** OnClick Listeners**/


    private class LocationFabListener implements View.OnClickListener {
        @Override
        public void onClick(View view) {
            grabCurrentUserLocation();
        }
    }

    private void grabCurrentUserLocation(){
        //Once this button is clicked, that means the user wants their current location in the center of the map.

        //This mess checks to see if we have permission, then we ask politely if we don't
        if (ContextCompat.checkSelfPermission(getActivity().getApplicationContext(), android.Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(getActivity(), new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                    PERMISSION_ACCESS_FINE_LOCATION);
        }
        //Once that permission has been granted, we obtain it; it not, we tell the user that's not possible.
        if (ContextCompat.checkSelfPermission(getActivity().getApplicationContext(), android.Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            beginGettingALocation();
        }
        //If no permission is granted, our activity will show a message!

    }

    @RequiresPermission(Manifest.permission.ACCESS_FINE_LOCATION)
    public void beginGettingALocation(){
        LocationManager lm = (LocationManager) getActivity().getSystemService(Context.LOCATION_SERVICE);
        //Use both the GPS and the Network provider to see who gets the update faster
        lm.requestLocationUpdates(LocationManager.GPS_PROVIDER,0,0,locationListener);
        lm.requestLocationUpdates(LocationManager.NETWORK_PROVIDER,0,0,locationListener);
    }

    private class MapLocationListener implements LocationListener{
        @Override public void onStatusChanged(String str, int value, Bundle bundle){}
        @Override public void onProviderEnabled(String provider){}
        @Override public void onProviderDisabled(String provider){}
        @Override public void onLocationChanged(Location _location){
            location = _location;
            //lm.requestSingleUpdate(LocationManager.GPS_PROVIDER,locationListener, Looper.getMainLooper());
            if (location != null) {
                //If the location is not null, center the map accordingly and highlight the point
                //But first clear our locationLayer
                locationLayer.removeAll();
                //Create a marker that's shaped like a circle
                Log.i("User Location" , location.getLatitude() + " " + location.getLongitude());
                SimpleMarkerSymbol simpleMarker = new SimpleMarkerSymbol(Color.BLUE, 10, SimpleMarkerSymbol.STYLE.CIRCLE);
                mapView.centerAt(location.getLatitude(), location.getLongitude(), false);
                Graphic pointOnMap = new Graphic(mapView.getCenter(),simpleMarker);
                locationLayer.addGraphic(pointOnMap);
                //Remove the listener once we're done
                ((LocationManager) getActivity().getSystemService(Context.LOCATION_SERVICE)).removeUpdates(locationListener);
            }
        }
    }

    private class HouseInfoOnClickListener implements View.OnClickListener{
        Intent i;
        public HouseInfoOnClickListener(Intent i){
            this.i = i;
        }
        @Override
        public void onClick(View view){
            //Here we convert from the map's coordinates to the traditional latitude longitude coordinates

            //Time to send them to the next activity (==( >|O
            startActivity(i);
        }
    }

    private class OnMapItemClickListener implements OnSingleTapListener{
        @Override
        public void onSingleTap(float x, float y) {
            int tolerance = 10; //To account for slight error
            //ie. If there's a point there
            mFeatureLayer.clearSelection(); //Clear any previous selection
            if (mFeatureLayer.getFeatureIDs(x, y, tolerance).length != 0) {
                //Essentially any points that fall within our selection will have their ID in this array
                long[] ids = mFeatureLayer.getFeatureIDs(x, y, tolerance);
                if (ids.length > 1) {//If we have more than one match, let's just say, for now, we found more than
                    //One match and call it a day
                    Toast.makeText(getActivity().getApplicationContext(), ids.length + " features found.", Toast.LENGTH_LONG).show();
                    return;
                }
                //If not, we highlight the feature on the map!
                mFeatureLayer.clearSelection();
                highlightHouseInfo(ids[0]);
            }
            else{//No matches, so let's hide the SnackBar
                snackBar.dismiss();
                mFeatureLayer.clearSelection();
            }
        }
    }
}