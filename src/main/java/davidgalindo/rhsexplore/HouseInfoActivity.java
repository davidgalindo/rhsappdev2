package davidgalindo.rhsexplore;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.webkit.WebView;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;

/**
 * Created by David on 5/3/2017.
 */

public class HouseInfoActivity extends Activity {
    String houseName;
    String houseAddress;
    String houseBuiltAwarded;
    String houseImageUrl;
    Bitmap image;
    String houseDesc;
    String websiteURL;

    public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.house_preview);
        Bundle intentBundle = getIntent().getExtras();
        houseName =  intentBundle.getString("houseName");
        houseAddress =  intentBundle.getString("houseAddress");
        houseBuiltAwarded = intentBundle.getString("houseBuiltAwarded");
        houseImageUrl = intentBundle.getString("houseImgUrl");
        websiteURL = intentBundle.getString("websiteURL");
        //Modify the text values
        ((TextView) findViewById(R.id.houseName)).setText(houseName);
        ((TextView) findViewById(R.id.houseAddress)).setText(houseAddress);
        ((TextView) findViewById(R.id.houseBuiltAwarded)).setText(houseBuiltAwarded);

        //Execute AsyncTasks
        new WebPageRetriever(websiteURL).execute();
        new ImageRetriever(houseImageUrl).execute();
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
            findViewById(R.id.progressBar).setVisibility(View.GONE);
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
}
