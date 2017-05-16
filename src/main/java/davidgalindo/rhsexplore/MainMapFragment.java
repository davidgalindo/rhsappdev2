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
import android.support.annotation.MainThread;
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
    private final int PERMISSION_ACCESS_FINE_LOCATION = 1;
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

    @Override
    public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        //Set up location listener for later
        lastCenter = null;
        locationListener = new MapLocationListener();
    }

    public void addFeatureAndGraphicLayer(GeodatabaseFeatureServiceTable _ft){
        //Receives these from the main activity
        ft = _ft;
        addLayers();
    }

    private void addLayers(){
        mapView.addLayer(mFeatureLayer = new FeatureLayer(ft));
        mapView.addLayer(locationLayer = new GraphicsLayer());
    }

    @Override
    public CoordinatorLayout onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
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
                    if(ft != null) {
                        setMapCenter();
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
        lastCenter = mapView.getCenter();
        mapView.pause();
    }
    @Override public void onResume() {super.onResume();mapView.unpause();if(ft!=null)addLayers();}

    private void setMapCenter(){
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(getActivity().getApplicationContext());
        String startingLocation = sp.getString("starting_point", "None!");
        String startURL = sp.getString("startURL","");
        Log.i("Starting location", "start: " + startURL);
        Log.i("Shared Preferences", startingLocation);
        if(lastCenter != null){
            mapView.centerAt(lastCenter, false);
        }
        else if (!startURL.equals("")){
            Log.i("query","Search for " + startURL);
            QueryParameters q = new QueryParameters();
            q.setWhere("WEBSITE LIKE '" + startURL +"'");
            if(ft.getStatus() != GeodatabaseFeatureServiceTable.Status.INITIALIZED){
                return;
            }
            ft.queryFeatures(q, new CallbackListener<FeatureResult>() {
                @Override
                public void onCallback(FeatureResult objects) {
                    if(objects.featureCount() > 0){
                        for(Object o: objects){
                            Feature feature = (Feature) o;
                            highlightHouseInfo(feature.getId());
                        }
                    }else{
                        Toast.makeText(getActivity().getApplicationContext(),"No results found.",Toast.LENGTH_SHORT).show();

                    }
                }

                @Override
                public void onError(Throwable throwable) {

                }
            });
        }
        else if (startingLocation.equals("currentlocation")) {
            grabCurrentUserLocation();
            Log.i("Shared Preferences", "Initialized to Current Location");
        } else {
            mapView.centerAndZoom(DOWNTOWN_REDLANDS.getX(), DOWNTOWN_REDLANDS.getY(), 18);
            Log.i("Shared Preferences", "Initialized to Downtown");
        }
    }


    private void highlightHouseInfo(long id){
        //Let's select our match and extract some data from it.
        Feature feature = mFeatureLayer.getFeature(id);
        String name = (String) feature.getAttributeValue("NAME");
        String address = (String) feature.getAttributeValue("Street");
        String picURL = (String) feature.getAttributeValue("PIC_URL");
        String builtAwarded ="Built " + feature.getAttributeValue("Year_Built")+ ", Awarded " + feature.getAttributeValue("Year_Awarded");
        String websiteURL = (String) feature.getAttributeValue("WEBSITE");
        long houseId = id;
        Point p = (Point) feature.getGeometry();
        mapView.centerAt(p,true);
        mFeatureLayer.selectFeature(id);
        HouseInfoOnClickListener onClickListener = new HouseInfoOnClickListener(name,address,builtAwarded,picURL,websiteURL,houseId);

        //Let's use a SnackBar to present the name in a less gaudy format
        snackBar = Snackbar.make(view,name,Snackbar.LENGTH_INDEFINITE);
        snackBar.setAction("INFO",onClickListener).show();
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
            }
        }
    }

    private class HouseInfoOnClickListener implements View.OnClickListener{
        String houseName;
        String houseAddress;
        String houseBuiltAwarded;
        String imgUrl;
        String websiteURL;
        long houseId;
        public HouseInfoOnClickListener(String name, String address, String builtAwarded,String url, String websiteURL, long houseId){
            houseAddress = address;
            houseName = name;
            houseBuiltAwarded = builtAwarded;
            imgUrl = url;
            this.websiteURL = websiteURL;
            this.houseId = houseId;
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
            i.putExtra("houseId",houseId);

            //Here we convert from the map's coordinates to the traditional latitude longitude coordinates
            String coords = CoordinateConversion.pointToDegreesMinutesSeconds(mapView.getCenter(),mapView.getSpatialReference(),6);
            i.putExtra("houseCoords",coords);
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
                highlightHouseInfo(ids[0]);


            }
            else{//No matches, so let's hide the SnackBar

                snackBar.dismiss();
            }
        }
    }
}