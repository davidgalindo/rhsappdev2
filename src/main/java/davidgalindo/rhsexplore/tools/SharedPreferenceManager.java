package davidgalindo.rhsexplore.tools;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

/**
 * Created by David on 5/17/2017.
 * Created to clean up some code and make it easier to access.
 */

public class SharedPreferenceManager {
    private SharedPreferences sp;
    private SharedPreferences.Editor editor;
    Context context;

    //Strings to keep track of what I call everything in this preferneces manager
    private final String IS_FIRST_BOOT = "first_boot";
    private final String JSON_FAVORITES = "jsonFavorites";
    private final String JSON_RECENTS = "jsonRecents";
    private final String JSON_RECENTS_SIZE = "recentsSize";
    private final String STARTING_POINT = "starting_point";
    private final String START_URL = "startURL";


    public SharedPreferenceManager(Context context){
        this.context = context;
        sp = PreferenceManager.getDefaultSharedPreferences(context);
        editor = sp.edit();
    }

    //First time boot
    public boolean isFirstBoot(){
        return sp.getBoolean(IS_FIRST_BOOT,true);
    }
    public void enableFirstBoot(){
        editor.putBoolean(IS_FIRST_BOOT,true).apply();
    }
    public void disableFirstBoot(){
        editor.putBoolean(IS_FIRST_BOOT,false).apply();
    }

    //JSON Favorites Array
    public String getJSONFavorites(){return sp.getString(JSON_FAVORITES,"");}
    public void setJSONFavorites(String s){
        editor.putString(JSON_FAVORITES,s).apply();
    }

    //JSON Recents Array
    public String getJSONRecents(){return sp.getString(JSON_RECENTS,"");}
    public void setJSONRecents(String s){
        editor.putString(JSON_RECENTS,s).apply();
    }

    //JSON Recents Size
    public int getJSONRecentsSize(){return sp.getInt(JSON_RECENTS_SIZE,5);}
    public void setJSONRecentsSize(int value){editor.putInt(JSON_RECENTS_SIZE,value);}

    //Starting Point
    public String getStartingPoint(){return sp.getString(STARTING_POINT,"");}
    public void setStartingPoint(String s){
        editor.putString(STARTING_POINT,s).apply();
    }

    //Starting URL
    public String getStartURL(){return sp.getString(START_URL,"");}
    public void setStartURL(String s){
        editor.putString(START_URL,s).apply();
    }
}
