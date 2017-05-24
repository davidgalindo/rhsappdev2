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

import com.esri.core.geodatabase.GeodatabaseFeatureServiceTable;
import com.esri.core.map.CallbackListener;
import com.esri.core.map.Feature;
import com.esri.core.map.FeatureResult;
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
    Button filterBtn;
    ListView rootView;
    HouseAdapter itemsAdapter;
    ArrayList<House> houseList, filteredHouseList, searchResults;
    protected CharSequence[] receivers = {"pre-1880", "1880-1889", "1890-1899", "1900-1909", "1910-1919", "1920-1929", "1930-1939"};
    protected ArrayList<CharSequence> selectedReceivers = new ArrayList<>();
    EditText searchText;
    long featureCount;
    int count;
    String searchTextString;
    String s;
    int checkHouseList, checkSearch, checkFiltered;
    ProgressBar progressBar;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        //this is just so I can access views from the xml files
        view = inflater.inflate(R.layout.house_list, container, false);
        Log.i("views", "" + (view == null));
        houseList = new ArrayList<House>();
        filteredHouseList = new ArrayList<House>();
        searchResults = new ArrayList<House>();
        rootView = (ListView) view.findViewById(R.id.houseListRV);
        header = inflater.inflate(R.layout.house_list_header, rootView, false);

        ft = ((MainActivity) getActivity()).getFeatureTable();

        checkHouseList = 0;
        createArrayAdapterWithHouseList();
        queryTableElements();

        return view;
    }

    public void searchBtnOnClick(EditText _searchText, String _searchTextString) {
        checkSearch = 0;
        searchText = _searchText;
        searchTextString = _searchTextString;

        createArrayAdapterWithSearchResults();
        queryTableElements();
        Log.i("onclick", "successsss");
    }

    public void createFilterDialog() {
        checkFiltered = 0;
        createArrayAdapterWithFilteredHouseList();
        final boolean[] checkedReceivers = new boolean[receivers.length];
        //set all receivers initially to false
        for (int i = 0; i < receivers.length; i++)
            checkedReceivers[i] = false;

        Log.i("selecteddecades", "sucess");
        selectedReceivers.clear();
        filteredHouseList.clear();
        s = "";

        Log.i("selectedreceivers", Integer.toString(selectedReceivers.size()));
        DialogInterface.OnMultiChoiceClickListener receiversDialogListener = new DialogInterface.OnMultiChoiceClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int position, boolean isChecked) {
                if (isChecked) {
                    if (!selectedReceivers.contains(receivers[position]))
                        selectedReceivers.add(receivers[position]);
                } else {
                    if (selectedReceivers.contains(receivers[position]))
                        selectedReceivers.remove(receivers[position]);
                }
                int count = receivers.length;
                for (int i = 0; i < count; i++)
                    checkedReceivers[i] = selectedReceivers.contains(receivers[i]);
            }
        };

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder
                .setTitle("Select Decade Built")
                .setMultiChoiceItems(receivers, checkedReceivers, receiversDialogListener)
                .setCancelable(false)
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        queryTableElements();
                    }
                });
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    public void queryTableElements() {
        Log.i("query", "succes");
        count = 0;
        featureCount = 1;
        QueryParameters q = new QueryParameters();
        q.setWhere("State LIKE '" + "CA" + "'");
        if (ft.getStatus() != GeodatabaseFeatureServiceTable.Status.INITIALIZED) {
            Log.i("initialize", "fail");
            return;
        }
        ft.queryFeatures(q, new CallbackListener<FeatureResult>() {
            @Override
            public void onCallback(FeatureResult objects) {
                if (objects.featureCount() > 0) {
                    featureCount = objects.featureCount();
                    for (Object o : objects) {
                        Log.i("FeatureResult", Long.toString(objects.featureCount()));
                        count++;
                        Feature feature = (Feature) o;
                        highlightHouseInfo(feature);
                    }
                    Log.i("Houselist", Integer.toString(houseList.size()));
                    sortAlphabetically(houseList);
                    sortAlphabetically(searchResults);
                    sortYearBuilt(filteredHouseList);
                    itemsAdapter.notifyDataSetChanged();

                    checkHouseList = 0;
                    checkFiltered = 0;
                    checkSearch = 0;

                } else {
                    Toast.makeText(getActivity().getApplicationContext(), "No results found.", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onError(Throwable throwable) {
            }
        });
        if (checkHouseList == 1) {
            do {
                itemsAdapter.notifyDataSetChanged();
                Log.i("notify", "succesS");
                if(count == featureCount) {
                    break;
                }
            }while(true);
        }

    }

    public void createArrayAdapterWithHouseList() {
        checkHouseList++;
        itemsAdapter = new HouseAdapter(getActivity(), houseList);
        rootView.addHeaderView(header, null, false);
        rootView.setAdapter(itemsAdapter);
    }

    public void createArrayAdapterWithSearchResults() {
        checkSearch++;
        searchResults.clear();
        itemsAdapter = new HouseAdapter(getActivity(), searchResults);
        // rootView.addHeaderView(header, null, false);
        rootView.setAdapter(itemsAdapter);

    }

    public void createArrayAdapterWithFilteredHouseList() {
        checkFiltered++;
        itemsAdapter = new HouseAdapter(getActivity(), houseList);
        //rootView.addHeaderView(header, null, false);
        //rootView.setAdapter(itemsAdapter);
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
        String decadeBuilt = (String) feature.getAttributeValue("Decade");

        if (checkHouseList == 1) {
            houseList.add(new House(picURL, name, yearBuilt, houseURL));
            Log.i("housearray", Integer.toString(houseList.size()));
        }

        if (checkSearch == 1) {
            if (name.toLowerCase().contains(searchTextString)) {
                searchResults.add(new House(picURL, name, yearBuilt, houseURL));
            }
        }

        //Log.i("search",Integer.toString(searchTextString.length()));
        //Log.i("searcharray",Integer.toString(searchResults.size()));

        if (checkFiltered == 1) {
            if (selectedReceivers.contains(decadeBuilt)) {
                //filteredHouseList.add(new House(picURL, name, yearBuilt,houseURL));
                if (!houseList.contains(feature)) {
                    Log.i("filtered","add");
                    houseList.add(new House(picURL, name, yearBuilt, houseURL));
                }
            } else {
                Log.i("filtered","remove1");
//selectedReceivers doesn't contain decadeBuilt
                if (houseList.contains(feature)) {
                    Log.i("filtered","remove");
                    houseList.remove(feature);
                }
            }
        }
    }
}
