package davidgalindo.rhsexplore;
import android.app.AlertDialog;
import android.app.Fragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.SyncStateContract;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Filter;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.esri.android.map.FeatureLayer;
import com.esri.core.geodatabase.GeodatabaseFeatureServiceTable;
import com.esri.core.map.CallbackListener;
import com.esri.core.map.Feature;
import com.esri.core.map.FeatureResult;
import com.esri.core.table.FeatureTable;
import com.esri.core.tasks.query.QueryParameters;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;
import java.util.logging.Handler;


/**
 * Created by Sean on 5/13/2017.
 */
public class HouseListFragment extends Fragment {
    private GeodatabaseFeatureServiceTable ft;
    private View view, header;
    ListView rootView;
    HouseAdapter itemsAdapter;
    ArrayList<House> houseList, searchResults;
    protected CharSequence[] receivers = {"pre-1880", "1880-1889", "1890-1899", "1900-1909", "1910-1919", "1920-1929", "1930-1939"};
    private HashMap<Decades,Boolean> enabledDecadesMap;
    EditText searchText;
    long featureCount;
    int count;
    String searchTextString;

    final boolean[] checkedReceivers = new boolean[receivers.length];


    //Finals for ease of use
    public static final int QUERY_ALL = 0;
    public static final int ALPHABETICALLY = 1;
    public static final int BY_DECADE = 2;
    public static final int BY_SEARCH_TERM = 4;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        //this is just so I can access views from the xml files
        view = inflater.inflate(R.layout.house_list, container, false);
        Log.i("views", "" + (view == null));
        houseList = new ArrayList<>();
        searchResults = new ArrayList<>();

        rootView = (ListView) view.findViewById(R.id.houseListRV);
        header = inflater.inflate(R.layout.house_list_header, rootView, false);
        header.findViewById(R.id.filterBtn).setOnClickListener(new OnFilterButtonClickListener());
        ft = ((MainActivity) getActivity()).getFeatureTable();

        rootView.addHeaderView(header, null, false);
        //createArrayAdapterWithHouseList();
        queryTableElements(QUERY_ALL,null);

        return view;
    }

    public void searchBtnOnClick(EditText _searchText, String _searchTextString) {
        searchText = _searchText;
        searchTextString = _searchTextString;

        queryTableElements(BY_SEARCH_TERM,searchTextString);
        Log.i("onclick", "Successfully clicked the search button!");
    }

    public void queryTableElements(int getWhat, String term) {
        //Set our spinner to visible and our rootview to invisible.
        view.findViewById(R.id.progressBar).setVisibility(View.VISIBLE);
        rootView.setVisibility(View.GONE);
        String whereClause;

        if(getWhat == ALPHABETICALLY){
            whereClause = "State LIKE '" + "CA" + "'";
        }else if(getWhat == BY_SEARCH_TERM){
            whereClause = "NAME LIKE '%" + term + "%' OR Street LIKE '%" + term + "%'";
        }else if (getWhat == BY_DECADE){
            //Figure out which decades we want and add those terms to our query
            whereClause = buildDecadeWhereClause();
        }
        else{//No criteria, just fill the list
            whereClause = "State LIKE '" + "CA" + "'";
        }
        Log.i("TableQuery",whereClause);
        TableQuery tq = new TableQuery(ft,whereClause);
        tq.run();
    }

    private String buildDecadeWhereClause(){
        String whereClause = "";
        //I have to hardcode this, but it should cover everything
        //6 Elements. For reference: "pre-1880", "1880-1889", "1890-1899", "1900-1909", "1910-1919", "1920-1929", "1930-1939"
        boolean previous = false;
        if(enabledDecadesMap.get(Decades.PRE_1880s)){
            whereClause += "(Year_Built_Numeric < 1880) ";
            previous = true;
        }
        if(enabledDecadesMap.get(Decades._1880_TO_1889)){
            if(previous){whereClause += " OR ";}
            whereClause += " (Year_Built_Numeric > 1880 AND Year_Built_Numeric < 1889)";
        }
        if(enabledDecadesMap.get(Decades._1890_TO_1899)){
            if(previous){whereClause += " OR ";}
            whereClause += " (Year_Built_Numeric > 1890 AND Year_Built_Numeric < 1899)";
        }
        if(enabledDecadesMap.get(Decades._1900_TO_1909)){
            if(previous){whereClause += " OR ";}
            whereClause += " (Year_Built_Numeric > 1900 AND Year_Built_Numeric < 1909)";
        }
        if(enabledDecadesMap.get(Decades._1910_TO_1919)){
            if(previous){whereClause += " OR ";}
            whereClause += " (Year_Built_Numeric > 1910 AND Year_Built_Numeric < 1919)";
        }
        if(enabledDecadesMap.get(Decades._1920_TO_1929)){
            if(previous){whereClause += " OR ";}
            whereClause += " (Year_Built_Numeric > 1920 AND Year_Built_Numeric < 1929)";
        }
        if(enabledDecadesMap.get(Decades._1930_TO_1939)){
            if(previous){whereClause += " OR ";}
            whereClause += " (Year_Built_Numeric > 1930 AND Year_Built_Numeric < 1939)";
        }
        return whereClause;
    }

    public void createArrayAdapterWithHouseList() {
        itemsAdapter = new HouseAdapter(getActivity(), houseList);
        rootView.setAdapter(itemsAdapter);
    }

    private void sortAlphabetically(ArrayList<House> list) {
        Collections.sort(list, new Comparator<House>() {
            public int compare(House h1, House h2) {
                return h1.getName().compareTo(h2.getName());
            }
        });
    }

    private void sortYearBuilt(ArrayList<House> list) {
        Collections.sort(list, new Comparator<House>() {
            public int compare(House h1, House h2) {
                return h1.getYearBuilt().compareTo(h2.getYearBuilt());
            }
        });
    }

    private void highlightHouseInfo(Feature feature) {
        //Let's select our match and extract some data from it.
        String name = (String) feature.getAttributeValue("NAME");
        String picURL = (String) feature.getAttributeValue("THUMB_URL");
        String yearBuilt = (String) feature.getAttributeValue("Year_Built");
        String houseURL = (String) feature.getAttributeValue("WEBSITE");
        String houseAddress = (String) feature.getAttributeValue("Street");
        //String decadeBuilt = (String) feature.getAttributeValue("Decade");

        House house = new House(picURL, name, yearBuilt, houseURL,houseAddress);
        houseList.add(house);
    }


    private void initializeHashMap(){
        enabledDecadesMap = new HashMap<>();
        enabledDecadesMap.put(Decades.PRE_1880s,false);
        enabledDecadesMap.put(Decades._1880_TO_1889,false);
        enabledDecadesMap.put(Decades._1890_TO_1899,false);
        enabledDecadesMap.put(Decades._1900_TO_1909,false);
        enabledDecadesMap.put(Decades._1910_TO_1919,false);
        enabledDecadesMap.put(Decades._1920_TO_1929,false);
        enabledDecadesMap.put(Decades._1930_TO_1939,false);
    }

    private class OnFilterButtonClickListener implements View.OnClickListener{
        @Override
        public void onClick(View view){
            initializeHashMap();
            DialogInterface.OnMultiChoiceClickListener receiversDialogListener = new DialogInterface.OnMultiChoiceClickListener() {

                @Override
                public void onClick(DialogInterface dialog, int position, boolean isChecked) {
                    Decades toModify = Decades.values()[position];
                    Log.i("Decade ", ""+toModify);
                    if (isChecked) {
                        enabledDecadesMap.put(toModify,true);
                    } else {
                        enabledDecadesMap.put(toModify,false);
                    }
                }
            };

            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder
                    .setTitle("Select Decade Built")
                    .setMultiChoiceItems(receivers, checkedReceivers, receiversDialogListener)
                    .setCancelable(true).setNegativeButton("Cancel",null)
                    .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            //Here we query the table with our choices
                            //emptyArrayAdapter();
                            queryTableElements(BY_DECADE, null);
                        }
                    });
            AlertDialog dialog = builder.create();
            dialog.show();
        }

    }
    private class TableQuery implements CallbackListener<FeatureResult>{
        private String whereClause;
        private GeodatabaseFeatureServiceTable featureServiceTable;
        private QueryParameters q;
        public TableQuery(GeodatabaseFeatureServiceTable _ft, String clause){
            featureServiceTable = _ft;
            whereClause = clause;
            q = new QueryParameters();
            q.setWhere(whereClause);
        }

        public void run(){
            if (featureServiceTable.getStatus() != GeodatabaseFeatureServiceTable.Status.INITIALIZED) {
                Log.i("initialize", "fail");
                return;
            }
            ft.queryFeatures(q,this);


        }
        @Override
        public void onCallback(FeatureResult objects) {
            Log.i("FeatureTable","Query called back!");
            if (objects.featureCount() > 0) {
                houseList.clear();
                //itemsAdapter.notifyDataSetChanged();
                Log.i("FeatureTable","Looking at features...");
                featureCount = objects.featureCount();
                for (Object o : objects) {
                    count++;
                    Feature feature = (Feature) o;
                    highlightHouseInfo(feature);
                }
                Log.i("FeatureTable","Query run and fetched!");
                Log.i("Houselist", Integer.toString(houseList.size()));
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        createArrayAdapterWithHouseList();
                        //Set spinner invisible and our house list visible
                        view.findViewById(R.id.progressBar).setVisibility(View.GONE);
                        rootView.setVisibility(View.VISIBLE);
                    }
                });

                //itemsAdapter.notifyDataSetChanged();
                //itemsAdapter.clear();
                //itemsAdapter.addAll(houseList);
                //itemsAdapter.notifyDataSetChanged();

            } else {
                Toast.makeText(getActivity().getApplicationContext(), "No results found.", Toast.LENGTH_SHORT).show();
            }
        }

        @Override
        public void onError(Throwable throwable) {
        }

    }

    //enum to keep track of our decades
    private enum Decades{
        PRE_1880s(0), _1880_TO_1889(1), _1890_TO_1899(2), _1900_TO_1909(3), _1910_TO_1919(4), _1920_TO_1929(5),
        _1930_TO_1939(6);
        private int value;

        private Decades(int value){
            this.value = value;
        }

    }
}
