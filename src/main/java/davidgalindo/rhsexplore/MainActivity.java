package davidgalindo.rhsexplore;

import android.app.AlertDialog;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;


import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;

import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.support.v7.app.ActionBarDrawerToggle;
import android.util.Log;
import android.view.MenuItem;
import android.widget.Toast;

import com.esri.android.map.GraphicsLayer;
import com.esri.android.runtime.ArcGISRuntime;
import com.esri.core.geodatabase.GeodatabaseFeatureServiceTable;
import com.esri.core.geometry.Point;
import com.esri.core.map.CallbackListener;

import davidgalindo.rhsexplore.tools.ConnectivityChecker;
import davidgalindo.rhsexplore.tools.SharedPreferenceManager;

/**
 * TODO: (no particular order priority)
 * (Handled by 3rd party app intent) === Add Navigation (ie. directions from current location to desired house)
 * (Working) === Have this app receive Intents (eg. from an external link, perhaps by name/id/coordinates) to directly link to a house
 * (Sean's part, share working) Social Features - Favorite a favorite house, recents list, share house to FB/Twitter, etc.
 * (Working) === Check for Internet connectivity upon boot
 * (Working, made welcome screen instead) Make FAQ
 * (Sean's part) Implement search/sort/display functionality (by decade, building type, etc.)
 * (Probably not necessary) Adjust layout for larger screens
 * (Always working on this!) Add polish (nicer basemap, better icons)
 *
 * Top Priority:
 * ( )Get app ready for release:
 * Remove developer version from map and implement an ESRI license
 * Request copyright permissions from RAHS to submit to Google
 * Clean up Logcats and debug toasts
 * TODO: End Todo list
 * **/


public class MainActivity extends AppCompatActivity {

    private Toolbar t;
    private DrawerLayout drawerLayout;
    private NavigationView nvDrawer;
    private ActionBarDrawerToggle drawerToggle;
    private MainMapFragment mainMapFragment;
    private GeodatabaseFeatureServiceTable ft;
    private GraphicsLayer gl;
    private Point lastPoint;
    private SharedPreferenceManager sp;
    private boolean duringBoot;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        //Present the Splash Screen to the user while the app loads
        sp = new SharedPreferenceManager(getApplicationContext());
        duringBoot = true;
        //TODO: make it look better
        setTheme(R.style.SplashTheme);

        //Check for internet first, then proceed only if internet was found.
        checkForInternet();

        //Once that's all done with, initialize our layout and content
        setTheme(R.style.AppTheme);
        super.onCreate(savedInstanceState);
        //Licensing - Standard level might require some features, but let's try this first
        String clientId = getResources().getString(R.string.client_id);
        ArcGISRuntime.setClientId(clientId);

        //Checks to see if this is the user's first time here (CURRENTLY IT WILL ONLY BOOT HERE, CHANGE IT!)
        checkForFirstBoot();
        //Now we check to see if we have an intent coming in
        checkForIntent();
        //Initialize the mapFragment
        mainMapFragment = new MainMapFragment();
        //From here, we initialize our FeatureLayers
        initializeFeatureLayers();
        gl = new GraphicsLayer();

        setContentView(R.layout.activity_main);


        //Create references to drawer, toolbar, etc.
        drawerLayout = (DrawerLayout) findViewById(R.id.drawerLayout);
        nvDrawer = (NavigationView) findViewById(R.id.nvView);
        t = (Toolbar) findViewById(R.id.toolBar);
        drawerToggle = setupDrawerToggle();
        drawerLayout.addDrawerListener(drawerToggle);
        t.setNavigationIcon(R.drawable.ic_list_white_24dp);
        setSupportActionBar(t);
        setupDrawerContent(nvDrawer);

    }


    private void checkForFirstBoot(){
        //if(firstBoot)
        if(sp.isFirstBoot()) {
            //If this is the first time the user is booting up, show them the welcome screen!
            Intent i = new Intent(this, WelcomeActivity.class);
            sp.disableFirstBoot();
            startActivity(i);
        }

    }


    public Point getPoint(){
        return lastPoint;
    }
    public void setPoint(Point p ){
        lastPoint = p;
    }

    private void initializeFeatureLayers(){
        ft = new GeodatabaseFeatureServiceTable("http://services7.arcgis.com/bRDoEnap5EYRc1GS/ArcGIS/rest/services/Houses/FeatureServer", 0);
        //Now we gotta initialize the Table to make sure our data is accessed correctly.
        ft.initialize(new CallbackListener<GeodatabaseFeatureServiceTable.Status>() {
            @Override
            public void onCallback(GeodatabaseFeatureServiceTable.Status status) {
                if (status == GeodatabaseFeatureServiceTable.Status.INITIALIZED) { //eg. A success
                    Log.i("FeatureLayer", "success");
                    //mainMapFragment.addFeatureAndGraphicLayer(ft);

                    //If this was a success, then we show/hide the UI

                    initialFragmentTransaction();

                }
            }
            @Override
            public void onError(Throwable throwable) {
                Toast.makeText(getApplicationContext(), "Error while loading data. Please try again later.", Toast.LENGTH_LONG).show();
            }
        });
    }

    private void initialFragmentTransaction(){
        //Responsible for the initial setup and transaction

        //Initial fragment
        FragmentManager fm = getFragmentManager();
        FragmentTransaction fragmentTransaction = fm.beginTransaction();
        fragmentTransaction.add(R.id.contentFrame,mainMapFragment).commit();
    }

    private String checkForIntent(){
        /**Allows the app to receive intents from the website.
         * eg, we got ohttp://rahs.org/awards/Elizabeth-Marshall-House/ and it can open up the appropriate
         * page inside the app.
         * We need to upload a file to the website first, so we can't do any work here, yet.
         * **/
        Intent appLinkIntent = getIntent();
        if(appLinkIntent == null) { //No incoming intent, do nothing
            sp.setStartURL("");
            return null;
        }
        Uri appLinkData = appLinkIntent.getData();
        if(appLinkData == null) {
            sp.setStartURL("");

            return null;
        }
        sp.setStartURL(appLinkData.toString());
        return appLinkData.toString();
    }

    //Get the FeatureTable for use in all Fragments
    public GeodatabaseFeatureServiceTable getFeatureTable(){
        return ft;
    }
    public GraphicsLayer getGraphicsLayer(){return gl;}

    private boolean checkForInternet(){
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(R.string.no_internet)
                .setPositiveButton(R.string.retry, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        if(checkForInternet())
                            startActivity(new Intent(getApplicationContext(),MainActivity.class));
                    }
                })
                .setNegativeButton(R.string.exit, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        finish();
                    }
                });
        AlertDialog dialog = builder.create();

        if (ConnectivityChecker.isConnectedToInternet(getApplicationContext())) {//ie. internet is  not available
            Log.i("Connectivity","No internet");
            dialog.show();
            return false;
        }else return true; // We have internet!

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // The action bar home/up action should open or close the drawer.
        switch (item.getItemId()) {
            case android.R.id.home:
                drawerLayout.openDrawer(GravityCompat.START);
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private ActionBarDrawerToggle setupDrawerToggle() {
        // NOTE: Make sure you pass in a valid toolbar reference.  ActionBarDrawToggle() does not require it
        // and will not render the hamburger icon without it.
        return new ActionBarDrawerToggle(this, drawerLayout, t, R.string.drawer_open,  R.string.drawer_close);
    }

    private void setupDrawerContent(NavigationView navigationView) {
        navigationView.setNavigationItemSelectedListener(
                new NavigationView.OnNavigationItemSelectedListener() {
                    @Override
                    public boolean onNavigationItemSelected(MenuItem menuItem) {
                        selectDrawerItem(menuItem);
                        return true;
                    }
                });
    }

    public void selectDrawerItem(MenuItem menuItem) {
        if(getTitle().equals(menuItem.getTitle())){
            //If the drawer item is already opened and the user clicks on it
            drawerLayout.closeDrawers();
            return;
        }

        // Create a new fragment and specify the fragment to show based on nav item clicked
        Fragment fragment = null;
        FragmentManager fm = getFragmentManager();
        FragmentTransaction fragmentTransaction = fm.beginTransaction();
        switch(menuItem.getItemId()) {
            case R.id.mainMap:
                fragment = mainMapFragment;
                break;
            case R.id.settingsMenu:
                fragment = new SettingsFragment();
                break;
            case R.id.houseList:
                fragment = new HouseListFragment();
                break;
            case R.id.favorites:
                fragment = SocialFragment.fromJsonArray(sp.getJSONFavorites());
                break;
            case R.id.recents:
                fragment = SocialFragment.fromJsonArray(sp.getJSONRecents());
                break;
            default:
                fragment = mainMapFragment;
        }

        if(fragment!= null) {
            fragmentTransaction.replace(R.id.contentFrame,fragment).setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN).commit();
        }
        // Highlight the selected item has been done by NavigationView
        menuItem.setChecked(true);

        // Set action bar title
        setTitle(menuItem.getTitle());
        // Close the navigation drawer
        drawerLayout.closeDrawers();
        //Precautionary step: check for internet
        checkForInternet();
    }

    @Override
    public void onBackPressed() {
        //Takes one back to the MainMapFragment when they press back
        //If they are already on the MainMapFragment, simply exit
        if (getFragmentManager().findFragmentById(mainMapFragment.getId()) == null){ // Not on MainMapFragment
            FragmentManager fm = getFragmentManager();
            FragmentTransaction fragmentTransaction = fm.beginTransaction();
            fragmentTransaction.replace(R.id.contentFrame,mainMapFragment).commit();
            MenuItem menuItem = nvDrawer.getMenu().getItem(0);
            menuItem.setChecked(true);
            setTitle(menuItem.getTitle());
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults){
        //Tell user to enable location services in case it was rejected
        switch(requestCode){
            case MainMapFragment.PERMISSION_ACCESS_FINE_LOCATION:
                if(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_DENIED){
                    Toast.makeText(getApplicationContext(), "Please enable Location Services for this app first, then try again.",
                            Toast.LENGTH_LONG).show();
                }else if(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){//Permission granted, yay! Let's get their location, then!
                    try {
                        mainMapFragment.beginGettingALocation();
                    }catch(SecurityException e){
                        e.printStackTrace();
                    }
                }
        }
    }

}
