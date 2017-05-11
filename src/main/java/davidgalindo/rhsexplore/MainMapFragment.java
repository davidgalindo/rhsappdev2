package davidgalindo.rhsexplore;

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
import android.os.Bundle;

import android.os.Looper;
import android.preference.PreferenceManager;
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
import com.esri.android.map.MapView;
import com.esri.android.map.event.OnSingleTapListener;
import com.esri.android.map.event.OnStatusChangedListener;
import com.esri.core.geodatabase.GeodatabaseFeatureServiceTable;

import com.esri.core.geometry.Point;
import com.esri.core.geometry.SpatialReference;
import com.esri.core.map.CallbackListener;
import com.esri.core.map.Feature;
import com.esri.core.map.Graphic;
import com.esri.core.symbol.SimpleMarkerSymbol;


/**
 * Created by David on 4/26/2017.
 * Fragment that contains all the functionality of the ArcGIS Map.
 */

public class MainMapFragment extends Fragment {
    private MapView mapView;
    private final int PERMISSION_ACCESS_FINE_LOCATION = 1;
    private CoordinatorLayout view;
    private GeodatabaseFeatureServiceTable ft;
    private FeatureLayer mFeatureLayer;
    private Snackbar snackBar;
    //Layer used for navigation drawing and current location mapping
    private GraphicsLayer locationLayer;
    private MapLocationListener locationListener;
    private Location location;
    private boolean firstBoot;
    private final String PREFS_NAME = "rhsPersistent";
    private final Point DOWNTOWN_REDLANDS = new Point(34.055569,-117.182538);

    private final String PORTAL_URL = "http://services7.arcgis.com/bRDoEnap5EYRc1GS/arcgis/rest/services/HousesTL/MapServer";

    @Override
    public CoordinatorLayout onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        view = (CoordinatorLayout) inflater.inflate(R.layout.arcgis_map_view, container, false);
        mapView = (MapView) view.findViewById(R.id.mapView);
        //Reads the settings and adjusts the map accordingly
        mapView.setOnStatusChangedListener(new OnStatusChangedListener() {
            @Override
            public void onStatusChanged(Object source, STATUS status) {
                if(status == STATUS.INITIALIZED){
                    //First initialization, set listeners here
                    mapView.enableWrapAround(true);
                    //Reacts when a user tries to select a point on the map.
                    mapView.setOnSingleTapListener(new OnMapItemClickListener());
                    //Set Precise Location Button Listener
                    FloatingActionButton preciseLocationBtn = (FloatingActionButton) view.findViewById(R.id.fab);
                    preciseLocationBtn.setOnClickListener(new LocationFabListener());
                }
                else if(status == STATUS.LAYER_LOADED) {
                    SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(getActivity().getApplicationContext());
                    String startingLocation = sp.getString("starting_point", "None!");
                    Log.i("Shared Preferences", startingLocation);
                     if (startingLocation.equals("currentlocation")) {
                        grabCurrentUserLocation();
                        Log.i("Shared Preferences", "Initialized to Current Location");
                    } else {
                        mapView.centerAndZoom(DOWNTOWN_REDLANDS.getX(), DOWNTOWN_REDLANDS.getY(), 18);
                        Log.i("Shared Preferences", "Initialized to Downtown");
                    }
                }else if (status == STATUS.INITIALIZATION_FAILED){
                    Log.i("Connection","Initialization failed");
                    Toast.makeText(getActivity().getApplicationContext(),"Unable to initialize map. Possible server issue. Please try again later.",Toast.LENGTH_LONG).show();
                }
            }
        });
        return view;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        firstBoot = true;
        //Set up location listener for later
        locationListener = new MapLocationListener();
        locationLayer = new GraphicsLayer();
        //Set up references
        snackBar = Snackbar.make(view,"blank",0);

        //Feature Table to create the FeatureLayer
        ft = new GeodatabaseFeatureServiceTable("http://services7.arcgis.com/bRDoEnap5EYRc1GS/ArcGIS/rest/services/Houses/FeatureServer", 0);
        //Now we gotta initialize the Table to make sure our data is accessed correctly.
        ft.initialize(new CallbackListener<GeodatabaseFeatureServiceTable.Status>() {
            @Override
            public void onCallback(GeodatabaseFeatureServiceTable.Status status) {
                if (status == GeodatabaseFeatureServiceTable.Status.INITIALIZED) { //eg. A success
                    //Then assign the table to a FeatureLayer and then place it onto the map
                    mFeatureLayer = new FeatureLayer(ft);
                    mapView.addLayer(mFeatureLayer);
                    //Also add graphicsLayer here since the app won't work without the featureLayer
                    mapView.addLayer(locationLayer);
                }
            }
            @Override
            public void onError(Throwable throwable) {
                Toast.makeText(getActivity().getApplicationContext(), "Error while loading data. Please try again later.", Toast.LENGTH_LONG).show();
            }
        });

    }

    @Override
    public void onPause() {
        super.onPause();
        mapView.pause();
    }

    @Override
    public void onResume() {
        super.onResume();
        mapView.unpause();
    }


    /** OnClick Listeners**/

    private class GetDirectionsListener implements View.OnClickListener{
        @Override
        public void onClick(View view){

        }
    }

    private class LocationFabListener implements View.OnClickListener {
        @Override
        public void onClick(View view) {
            grabCurrentUserLocation();
        }
    }

    private void grabCurrentUserLocation(){
        //Once this button is clicked, that means the user wants their current location in the center of the map.
        LocationManager lm = (LocationManager) getActivity().getSystemService(Context.LOCATION_SERVICE);
        //This mess checks to see if we have permission, then we ask politely if we don't
        if (ContextCompat.checkSelfPermission(getActivity().getApplicationContext(), android.Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(getActivity(), new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                    PERMISSION_ACCESS_FINE_LOCATION);
        }
        //Once that permission has been granted, we obtain it; it not, we tell the user that's not possible.
        if (ContextCompat.checkSelfPermission(getActivity().getApplicationContext(), android.Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            lm.requestSingleUpdate(lm.getBestProvider(new Criteria(), true),locationListener, Looper.getMainLooper());
        } else {
            Toast.makeText(getActivity().getApplicationContext(), "Please enable Location Services for this app first, then try again.",
                    Toast.LENGTH_LONG).show();
        }

    }

    private class MapLocationListener implements LocationListener{
        @Override
        public void onStatusChanged(String str, int value, Bundle bundle){

        }
        @Override
        public void onProviderEnabled(String provider){

        }
        @Override
        public void onProviderDisabled(String provider){

        }
        @Override
        public void onLocationChanged(Location _location){
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
            }
        }
    }

    private class HouseInfoOnClickListener implements View.OnClickListener{
        String houseName;
        String houseAddress;
        String houseBuiltAwarded;
        String imgUrl;
        String websiteURL;
        public HouseInfoOnClickListener(String name, String address, String builtAwarded,String url, String websiteURL){
            houseAddress = address;
            houseName = name;
            houseBuiltAwarded = builtAwarded;
            imgUrl = url;
            this.websiteURL = websiteURL;
        }
        @Override
        public void onClick(View view){
            //Set up an intent to pass on information to our HouseInfoActivity
            Intent i = new Intent(getActivity(), HouseInfoActivity.class);
            i.putExtra("houseName",houseName);
            i.putExtra("houseAddress",houseAddress);
            i.putExtra("houseBuiltAwarded",houseBuiltAwarded);
            i.putExtra("houseImgUrl",imgUrl);
            i.putExtra("websiteURL",websiteURL);
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

                //If not, let's give specific information about the selection.
                //Now we highlight the first feature
                mFeatureLayer.selectFeature(ids[0]);
                //Let's select our match and extract some data from it.
                Feature feature = mFeatureLayer.getFeature(ids[0]);
                String name = (String) feature.getAttributeValue("NAME");
                String address = (String) feature.getAttributeValue("Street");
                String picURL = (String) feature.getAttributeValue("PIC_URL");
                String builtAwarded ="Built " + feature.getAttributeValue("Year_Built")+ ", Awarded " + feature.getAttributeValue("Year_Awarded");
                String websiteURL = (String) feature.getAttributeValue("WEBSITE");

                Point p = (Point) feature.getGeometry();

                mapView.centerAt(p,true);
                HouseInfoOnClickListener onClickListener = new HouseInfoOnClickListener(name,address,builtAwarded,picURL,websiteURL);

                //Log.i("House Info", name + "\n" + address + "\n" + cityZip);

                //Let's use a SnackBar to present the name in a less gaudy format
                snackBar = Snackbar.make(view,name,Snackbar.LENGTH_INDEFINITE);
                snackBar.setAction("INFO",onClickListener).show();

            }
            else{//No matches, so let's hide the SnackBar

                snackBar.dismiss();
            }
        }
    }
}

