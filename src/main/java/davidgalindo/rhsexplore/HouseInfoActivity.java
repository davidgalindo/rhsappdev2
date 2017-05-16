package davidgalindo.rhsexplore;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;

import android.util.Log;
import android.view.View;
import android.webkit.WebView;
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
    SharedPreferences sp;
    JSONArray jsonRecents;
    long houseId;

    public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        //We pull up SharedPreferences here to see if we have an initial JSON String
        sp = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());



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

    }

    private void addToRecentsArray(){
        //The string representing our JSON array for our recents list
        String jsonRecentsString = sp.getString("jsonRecents","");
        try{
            //Keeps track of whether the value is already in the array
            boolean notInArray = true;
            //Create the array - if it doesn't exist, create it
            if(jsonRecentsString.equals("")) {
                jsonRecents = new JSONArray();
            }else{
                jsonRecents = new JSONArray(jsonRecentsString);
            }
            if(jsonRecents.length() >= sp.getInt("recentsSize",5)){//Default size of 15
                //If the array is larger than the recentsSize, remove the first element
                jsonRecents.remove(0);
            }
            for(int count=0; count<jsonRecents.length();count++){
                JSONObject o = (JSONObject) jsonRecents.get(count);
                long value = o.getLong("id");
                if(value == houseId){
                    notInArray = false;
                }
            }
            //Now we add the object to the json list as a recent
            if(notInArray) {
                JSONObject object = new JSONObject();
                object.put("id", houseId);
                jsonRecents.put(object);

                //Then we save the jsonArray to the SharedPreferences
                sp.edit().putString("jsonRecents", jsonRecents.toString()).apply();
                Log.i("json",jsonRecents.toString());
            }
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
        Toast.makeText(getApplicationContext(),"Added to favorites!",Toast.LENGTH_SHORT).show();
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
