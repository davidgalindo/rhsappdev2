package davidgalindo.rhsexplore;

import android.app.AlertDialog;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;

import android.content.DialogInterface;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.support.design.widget.NavigationView;


import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;

import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.support.v7.app.ActionBarDrawerToggle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

/**
 * TODO: (no particular order priority)
 * Add Navigation (ie. directions from current location to desired house)
 * Have this app receive Intents (eg. from an external link, perhaps by name/id/coordinates) to directly link to a house
 * Implement search/sort/display functionality (by decade, building type, etc.)
 * Social Features - Star a favorite house, share house to FB/Twitter, etc.
 * Make FAQ
 * Adjust layout for larger screens
 * (*) Check for Internet connectivity upon boot
 * Add polish (nicer basemap, better icons)
 * TODO: End ToDo list
 * **/


public class MainActivity extends AppCompatActivity {

    private Toolbar t;
    private DrawerLayout drawerLayout;
    private NavigationView nvDrawer;
    private ActionBarDrawerToggle drawerToggle;
    private MainMapFragment mainMapFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
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

        //Check for internet first, then proceed only if internet was found.
        checkForInternet();

    }

    private void initialFragmentTransaction(){
        //Responsible for the initial setup and transaction

        //Initial fragment
        FragmentManager fm = getFragmentManager();
        FragmentTransaction fragmentTransaction = fm.beginTransaction();

        mainMapFragment = new MainMapFragment();
        fragmentTransaction.add(R.id.contentFrame,mainMapFragment).commit();

        //Once we're done, show the UI
        findViewById(R.id.loadingScreen).setVisibility(View.GONE);
        findViewById(R.id.activity_main_menu).setVisibility(View.VISIBLE);
    }

    private void checkForInternet(){
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(R.string.no_internet)
                .setPositiveButton(R.string.retry, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        checkForInternet();
                    }
                })
                .setNegativeButton(R.string.exit, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        finish();
                    }
                });
        AlertDialog dialog = builder.create();

        ConnectivityManager cm = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
        NetworkInfo ni = cm.getActiveNetworkInfo();
        if (ni == null || !ni.isConnected()) {//ie. internet is  not available
            Log.i("Connectivity","No internet");
            dialog.show();
        }
        else
        {
            initialFragmentTransaction();
        }

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
        menuItem.getItemId();
        FragmentManager fm = getFragmentManager();
        FragmentTransaction fragmentTransaction = fm.beginTransaction();
        switch(menuItem.getItemId()) {
            case R.id.mainMap:
                fragment = mainMapFragment;
                break;
            case R.id.settingsMenu:
                fragment = new SettingsFragment();
                break;
            case R.id.faqMenu:
                //fragmentClass = ThirdFragment.class;
                Toast.makeText(getApplicationContext(),"Open FAQ menu!",Toast.LENGTH_SHORT).show();
                break;
            case R.id.houseList:
                //fragmentClass = HouseListFragment.class
                Toast.makeText(getApplicationContext(),"Open House List menu!",Toast.LENGTH_SHORT).show();
                break;
            default:
                fragment = mainMapFragment;
        }

        if(fragment!= null) {
            fragmentTransaction.replace(R.id.contentFrame,fragment).commit();
        }
        // Highlight the selected item has been done by NavigationView
        menuItem.setChecked(true);

        // Set action bar title
        setTitle(menuItem.getTitle());
        // Close the navigation drawer
        drawerLayout.closeDrawers();
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

}
