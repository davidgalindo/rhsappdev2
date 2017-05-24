package davidgalindo.rhsexplore.tools;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import static android.content.Context.CONNECTIVITY_SERVICE;

/**
 * Created by David on 5/23/2017.
 */

public class ConnectivityChecker extends  BroadcastReceiver{
    public static boolean isConnectedToInternet(Context c){
        ConnectivityManager cm = (ConnectivityManager) c.getSystemService(CONNECTIVITY_SERVICE);
        NetworkInfo ni = cm.getActiveNetworkInfo();
        return ni == null || !ni.isConnected();
    }
    public static ConnectivityReceiverListener connectivityReceiverListener;
    @Override
    public void onReceive(Context c, Intent intent){
        ConnectivityManager cm = (ConnectivityManager) c
                .getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        boolean isConnected = activeNetwork != null
                && activeNetwork.isConnectedOrConnecting();

        if (connectivityReceiverListener != null) {
            connectivityReceiverListener.onNetworkConnectionChanged(isConnected);
        }
    }

    public interface ConnectivityReceiverListener {
        void onNetworkConnectionChanged(boolean isConnected);
    }
}
