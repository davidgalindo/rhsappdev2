package davidgalindo.rhsexplore;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;

import android.util.Log;
import android.view.View;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import davidgalindo.rhsexplore.tools.SharedPreferenceManager;

/**
 * Created by David on 5/3/2017.
 */

public class HouseInfoActivity extends Activity{
    String houseName;
    String houseAddress;
    String houseBuiltAwarded;
    String houseImageUrl;
    Bitmap image;
    String houseDesc;
    String websiteURL;
    String houseCoords;
    SharedPreferenceManager sp;
    JSONArray jsonRecents;
    long houseId;

    public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        //We pull up SharedPreferences here to see if we have an initial JSON String
        sp = new SharedPreferenceManager(getApplicationContext());

        setContentView(R.layout.house_preview);
        Bundle intentBundle = getIntent().getExtras();
        houseName =  intentBundle.getString("houseName");
        houseAddress =  intentBundle.getString("houseAddress");
        houseBuiltAwarded = intentBundle.getString("houseBuiltAwarded");
        houseImageUrl = intentBundle.getString("houseImgUrl");
        websiteURL = intentBundle.getString("websiteURL");
        houseCoords = intentBundle.getString("houseCoords");
        houseId = intentBundle.getLong("houseId");
        //Modify the text values
        ((TextView) findViewById(R.id.houseName)).setText(houseName);
        ((TextView) findViewById(R.id.houseAddress)).setText(houseAddress);
        ((TextView) findViewById(R.id.houseBuiltAwarded)).setText(houseBuiltAwarded);

        //Execute AsyncTasks
        new WebPageRetriever(websiteURL).execute();
        new ImageRetriever(houseImageUrl).execute();
        addToRecentsArray();
        checkIfInitiallyFavorite();

    }

    private void addToRecentsArray(){
        //The string representing our JSON array for our recents list
        String jsonRecentsString = sp.getJSONRecents();
        Log.i("jsonrecents",jsonRecentsString);
        long matchIndex;
        int recentsSize = sp.getJSONRecentsSize();

        try{
            //Keeps track of whether the value is already in the array
            //Create the array - if it doesn't exist, create it
            if(jsonRecentsString.equals("")) {
                jsonRecents = new JSONArray();
            }else{
                jsonRecents = new JSONArray(jsonRecentsString);
            }
            if(recentsSize == 0){//If no recents are desired, return here
                //Clear the array, if it exists
                while(jsonRecents.length() > 0){

                    jsonRecents.remove(0);
                }
                sp.setJSONRecents("");
                return;
            }
            if(jsonRecents.length() >= recentsSize){//Default size of 15
                //If the array is larger than the recentsSize, remove the first element
                while(jsonRecents.length() >= recentsSize){
                    jsonRecents.remove(0);//Remove all first elements until the size is correct
                }
            }
            matchIndex = hasHouseId(jsonRecents,houseId);

            //Now we add the object to the json list as a recent
            if(matchIndex <= -1) {//Object not in Recents Array
                JSONObject object = new JSONObject();
                object.put("id", houseId);
                jsonRecents.put(object);

                //Then we save the jsonArray to the SharedPreferences
                sp.setJSONRecents(jsonRecents.toString());

                Log.i("json",jsonRecents.toString());
            }//We ignore it if it is in the recents array
        }catch(JSONException e){
            e.printStackTrace();
        }
    }

    private class WebPageRetriever extends  AsyncTask<String, Void, String>{
        String link;
        private final String targetStartHTML = "<p style=\"text-align: justify;\">";
        private final String targetStartHTML2 = "<p>";
        private final String targetEndHTML = "</p>";
        public WebPageRetriever(String url){
            link = url;
        }
        @Override
        public String doInBackground(String... _url){
            houseDesc = "";
            try {
                URL url = new URL(link);
                BufferedReader br = new BufferedReader(new InputStreamReader(url.openStream()));
                String input;
                //Essentially read input while there is still input to read
                boolean readingHouseDesc = false;
                while((input = br.readLine()) != null){
                    if(input.contains(targetStartHTML) || input.contains(targetStartHTML2)){
                        readingHouseDesc = true;
                    }
                    if(readingHouseDesc) {

                        houseDesc += input;
                        if(input.contains(targetEndHTML)){
                            readingHouseDesc = false;
                        }
                    }
                }
                br.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return houseDesc;
        }

        @Override
        public void onPostExecute(String args){
            //Kinda proud of this - instead of removing any HTML from the String, why not load it into a WebView?
            //The following lines use the String full of HTML to load it into a WebView.
            WebView webView = (WebView) findViewById(R.id.webView);
            webView.loadDataWithBaseURL("",houseDesc,"text/html","UTF-8","");

            //Set the background for the WebView
            webView.setBackgroundColor(0x00000000);
            webView.setBackgroundResource(R.drawable.papyrus);
            //The image will take the longest to load, so after loading it, show everything and hide the progress bar
            findViewById(R.id.progressBar).setVisibility(View.GONE);
            findViewById(R.id.postLoadContent).setVisibility(View.VISIBLE);

        }

    }

    private class ImageRetriever extends AsyncTask<String, Void, Bitmap> {
        String name;
        public ImageRetriever(String _name){
            name = _name;
        }
        @Override
        public void onPreExecute(){
            //While everything is loading, show a spinner
            findViewById(R.id.progressBar).setVisibility(View.VISIBLE);
        }
        @Override
        public Bitmap doInBackground(String... _url) {
            String link = houseImageUrl;
            image = null;
            try {
                URL url = new URL(link);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setDoInput(true);
                connection.connect();
                InputStream inputStream = connection.getInputStream();
                BitmapFactory.Options options = new BitmapFactory.Options();
                //options.inPreferredConfig = Bitmap.Config.RGB_565;
                image = BitmapFactory.decodeStream(inputStream, null, options);
                inputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return image;
        }
        @Override
        public void onPostExecute(Bitmap args){
            //Add the image
            if(image!=null) {
                ((ImageView) findViewById(R.id.houseImage)).setImageBitmap(image);
            }

        }

    }

    /**Onclick Methods**/
    public void onDirectionsClick(View view){

        //Format the uri so that it can be interperetd by Google Maps and other Navigation apps
        String uri = "geo:0,0?q=" + houseCoords + "&addr=" + houseAddress +",Redlands,CA";
        Toast.makeText(getApplicationContext(),uri,Toast.LENGTH_SHORT).show();
        Intent intent = new Intent(
                Intent.ACTION_VIEW, Uri.parse(uri)
        );
        startActivity(intent);

    }

    public void onFavoriteClick(View view){
        modifyFavorites();
    }

    private void checkIfInitiallyFavorite(){
        try{
            sp.getJSONFavorites();
            String jsonFavoritesString = sp.getJSONFavorites();
            JSONArray jsonFavorites;
            //Keeps track of whether the value is already in the array

            //Create the array - if it doesn't exist, create it
            if(jsonFavoritesString.equals("")) {
                jsonFavorites = new JSONArray();
            }else{
                jsonFavorites = new JSONArray(jsonFavoritesString);
            }
            long matchIndex = hasHouseId(jsonFavorites,houseId);
            if(matchIndex>-1){//eg. We're not changing the value and the object is in the array
                //Then change the image to a filled heart instead and just return
                ((ImageView)findViewById(R.id.favoriteBtn)).setImageResource(R.drawable.ic_favorite_black_36dp);
            }

        }catch(JSONException e){
            e.printStackTrace();
        }
    }

    //Keeps track of whether the value is already in the array
    private void modifyFavorites(){//responsible for manipulating favorites, if true
        //If false, we simply change the favorites image accordingly
        //The string representing our JSON array for our favorites list
        String jsonFavoritesString = sp.getJSONFavorites();
        try{
            JSONArray jsonFavorites;


            //Create the array - if it doesn't exist, create it
            if(jsonFavoritesString.equals("")) {
                jsonFavorites = new JSONArray();
            }else{
                jsonFavorites = new JSONArray(jsonFavoritesString);
            }
            //Check to see if the house is already in the array
            long matchIndex = hasHouseId(jsonFavorites,houseId);

            //Now we add the object to the json list as a recent
            if(matchIndex <= -1) { //Object not found, add the id here
                //Change the view accordingly
                ((ImageView)findViewById(R.id.favoriteBtn)).setImageResource(R.drawable.ic_favorite_black_36dp);
                //Add it into the JSON array
                JSONObject object = new JSONObject();
                object.put("id", houseId);
                jsonFavorites.put(object);

                Toast.makeText(getApplicationContext(),"Added to favorites!",Toast.LENGTH_SHORT).show();
            }else{//If the House is in the favorites array, remove it
                jsonFavorites.remove((int)matchIndex);
                Toast.makeText(getApplicationContext(),"Removed from favorites!",Toast.LENGTH_SHORT).show();
                //Change the view accordingly
                ((ImageView)findViewById(R.id.favoriteBtn)).setImageResource(R.drawable.ic_favorite_border_black_36dp);
            }
            //Save the changes in the StoredPreferences
            sp.setJSONFavorites(jsonFavorites.toString());
            Log.i("json favorites",jsonFavorites.toString());
        }catch(JSONException e){
            e.printStackTrace();
        }
    }
    private long hasHouseId(JSONArray array, long id) throws JSONException{
        //Looks up a house ID within the given array; returns -1 if nothing is found
        for(int count=0; count<array.length();count++){
            JSONObject o = (JSONObject) array.get(count);
            long value = o.getLong("id");
            if(value == id){
                return count; //returns the index value if found
            }
        }
        //If not found, returns a -1
        return -1;
    }
    public void onShareClick(View view){
        //
        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_SEND);
        intent.putExtra(Intent.EXTRA_TEXT, getResources().getString(R.string.share_prefix) + websiteURL);
        intent.setType("text/plain");
        startActivity(
                Intent.createChooser(intent,getResources().getString(R.string.share_using))
        );
    }
}
