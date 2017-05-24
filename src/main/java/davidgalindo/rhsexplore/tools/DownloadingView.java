package davidgalindo.rhsexplore.tools;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;

import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.net.URL;



/**
 * Created by David on 5/23/2017.
 * A collection of classes that assist with the dowloading of an image.
 */

public class DownloadingView  {
    private ProgressBar progressBar;
    public void setProgressBar(ProgressBar p){
        progressBar = p;
    }
    public void download(String url, ImageView imageView){
        if(cancelPotentialDownload(url,imageView)){
            ImageRetriever task = new ImageRetriever(imageView);
            DownloadBitmap downloadBitmap = new DownloadBitmap(task);
            imageView.setImageDrawable(downloadBitmap);
            task.execute(url);
        }
    }

    //Cancels any potential download that may be occurring
    public boolean cancelPotentialDownload(String url, ImageView imageView){
        ImageRetriever task = getBitmapDownloaderTask(imageView);
        if (task != null) {
            String bitmapDownloadUrl = task.url;

            //If our bitmap download URL is the same as the provided URL, or null
            //We will allow the download to continue if the two URLS are identical
            if(url.equals(bitmapDownloadUrl) || (bitmapDownloadUrl) == null){
                task.cancel(true);
            }else{
                return false;
            }
        }
        return true;
    }

    private static ImageRetriever getBitmapDownloaderTask(ImageView imageView) {
        if (imageView != null) {
            Drawable drawable = imageView.getDrawable();
            if (drawable instanceof DownloadBitmap) {
                DownloadBitmap downloadedDrawable = (DownloadBitmap)drawable;
                return downloadedDrawable.getDownloadImageTask();
            }
        }
        return null;
    }


    public class DownloadBitmap extends ColorDrawable {
        private final WeakReference<ImageRetriever> downloadTaskReference;
        public DownloadBitmap(ImageRetriever task){
            super(Color.BLACK);
            downloadTaskReference = new WeakReference<>(task);
        }

        public ImageRetriever getDownloadImageTask(){return downloadTaskReference.get();}



    }
    public class ImageRetriever extends AsyncTask<String, Void, Bitmap> {
        private String url;
        private final WeakReference<ImageView> imageViewReference;
        public ImageRetriever(ImageView imageView){
            imageViewReference = new WeakReference<>(imageView);
        }

        @Override
        protected void onPreExecute(){
            if(imageViewReference != null){
                ImageView imageView = imageViewReference.get();
                ImageRetriever task = getBitmapDownloaderTask(imageView);
                if(this == task){
                    imageView.setVisibility(View.INVISIBLE);
                    if(progressBar != null)
                        progressBar.setVisibility(View.VISIBLE);
                }
            }
        }

        @Override
        protected Bitmap doInBackground(String... _url) {
            url = _url[0];
            try {
                URL link = new URL(url);
                HttpURLConnection connection = (HttpURLConnection) link.openConnection();
                connection.setDoInput(true);
                connection.connect();
                InputStream inputStream = connection.getInputStream();
                BitmapFactory.Options options = new BitmapFactory.Options();
                //options.inPreferredConfig = Bitmap.Config.RGB_565;
                final Bitmap image = BitmapFactory.decodeStream(inputStream, null, options);
                inputStream.close();
                return image;
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }
        }
        @Override
        protected void onCancelled(Bitmap bmp){
            Log.i("HouseAdapter","Async canceled!");
        }
        @Override
        protected void onPostExecute(Bitmap bitmap){
            //If the task was cancelled, reutrn null
            if(isCancelled()) {
                bitmap = null;
            }
            if(imageViewReference != null){
                ImageView imageView = imageViewReference.get();
                ImageRetriever task = getBitmapDownloaderTask(imageView);
                if(this == task){
                    imageView.setImageBitmap(bitmap);
                    imageView.setVisibility(View.VISIBLE);
                    if(progressBar != null)
                        progressBar.setVisibility(View.GONE);
                }

            }



            Log.i("HouseAdapter","Async completed.");

        }

    }
}
