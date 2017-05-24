package davidgalindo.rhsexplore;

/**
 * Created by David on 5/19/2017.
 */

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.Image;
import android.net.Uri;
import android.os.AsyncTask;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;

import davidgalindo.rhsexplore.tools.DownloadingView;

/**
 * Created by Sean on 5/4/2017.
 */
public class HouseAdapter extends ArrayAdapter<House> {

    public HouseAdapter(Context context, ArrayList<House> houseList) {
        super(context, 0, houseList);
        Log.i("HouseAdapter","Successfully created with " + houseList.size() + " elements");
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        final House currentHouse = getItem(position);
        Log.i("HouseAdapter","Recorded a house.");
        View listItemView = convertView;
        if (listItemView == null) {
            listItemView = LayoutInflater.from(getContext()).inflate(R.layout.list_item, parent, false);
        }
        Log.i("HouseAdapter","Created a view.");
        //Assign references
        ImageView image = (ImageView) listItemView.findViewById(R.id.imgview);
        TextView name_text = (TextView) listItemView.findViewById(R.id.txtview1);
        TextView year_built = (TextView) listItemView.findViewById(R.id.txtview2);
        ProgressBar progressBar = (ProgressBar) listItemView.findViewById(R.id.loadingBar);

        //Set the text
        name_text.setText(currentHouse.getName());
        year_built.setText(currentHouse.getYearBuilt());
        Log.i("HouseAdapter","References set.");
        //The fun part: Download the image asynchronously
        String url = currentHouse.getImageURL();
        DownloadingView dv = new DownloadingView();
        dv.setProgressBar(progressBar);
        dv.download(url,image);


        //new ImageRetriever(url, listItemView).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR,null);
        listItemView.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                Intent i = new Intent(getContext(),MainActivity.class);
                i.setData(Uri.parse(currentHouse.getHouseURL()));
                getContext().startActivity(i);
            }
        });
        Log.i("HouseAdapter", "successfully finished. " + currentHouse);

        return listItemView;
    }

}

