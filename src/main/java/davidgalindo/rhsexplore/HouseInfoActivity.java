package davidgalindo.rhsexplore;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

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
        private final String targetStartHTML = "<p>";
        private final String targetEndHTML = "</p>";
        public WebPageRetriever(String url){
            link = url;
        }
        @Override
        public String doInBackground(String... _url){
            houseDesc = "";
            try {
                URL url = new URL(link);

                //HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                //connection.setDoInput(true);
                //connection.connect();
                BufferedReader br = new BufferedReader(new InputStreamReader(url.openStream()));
                String input;
                //Essentially read input while there is still input to read
                boolean readingHouseDesc = false;

                while((input = br.readLine()) != null){
                    if(input.contains(targetStartHTML)){
                        readingHouseDesc = true;
                    }
                    if(readingHouseDesc) {
                        houseDesc += input;
                        if(input.contains(targetEndHTML)){
                            readingHouseDesc = false;
                        }
                    }
                    //Log.i("House Desc printing", input);
                }

                br.close();
            } catch (IOException e) {
                e.printStackTrace();
            }

            return houseDesc;
        }

        @Override
        public void onPostExecute(String args){
            //Objective: Parse the HTML given to us, remove the HTML from all <p> to </p>
            houseDesc = houseDesc.replace(targetStartHTML,"").replace(targetEndHTML,"\n\n");

            //Extract all the HTML from it, then use that as our house description
            ((TextView) findViewById(R.id.houseDescription)).setText(houseDesc);
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
            //Update UI Elements here.

            //Set all UI Elements here.
            //First hide the Spinner
            findViewById(R.id.progressBar).setVisibility(View.VISIBLE);



            //Add the image
            if(image!=null) {
                ((ImageView) findViewById(R.id.houseImage)).setImageBitmap(image);
            }


            //Until I find a better way of implementing it, we'll leave the description unmodified, for now.
            //((TextView) findViewById(R.id.houseDescription)).setText(houseName);


        }

    }
}
