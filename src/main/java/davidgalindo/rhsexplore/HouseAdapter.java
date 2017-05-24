package davidgalindo.rhsexplore;

/**
 * Created by David on 5/19/2017.
 */

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.util.ArrayList;

import davidgalindo.rhsexplore.tools.DownloadingView;

/**
 * Created by Sean on 5/4/2017.
 */
public class HouseAdapter extends ArrayAdapter<House> {

    public HouseAdapter(Context context, ArrayList<House> houseList) {
        super(context, 0, houseList);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        final House currentHouse = getItem(position);
        View listItemView = convertView;
        if (listItemView == null) {
            listItemView = LayoutInflater.from(getContext()).inflate(R.layout.list_item, parent, false);
        }
        //Assign references
        ImageView image = (ImageView) listItemView.findViewById(R.id.imgview);
        TextView name_text = (TextView) listItemView.findViewById(R.id.txtview1);
        TextView year_built = (TextView) listItemView.findViewById(R.id.txtview2);
        TextView houseAddress = (TextView) listItemView.findViewById(R.id.houseAddress);
        ProgressBar progressBar = (ProgressBar) listItemView.findViewById(R.id.loadingBar);

        //Set the text
        name_text.setText(currentHouse.getName());
        year_built.setText(currentHouse.getYearBuilt());
        houseAddress.setText(currentHouse.getAddress());
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

        return listItemView;
    }

}
