package davidgalindo.rhsexplore;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * Created by David on 5/17/2017.
 */

public class WelcomeActivity extends AppCompatActivity {
    private ViewPager viewPager;
    private LinearLayout dots;
    private Button btnSkip;
    private Button btnNext;
    private int[] myLayouts;
    private Intent i;
    private ViewPageAdapter adapter;

    @Override
    public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.welcome_layout_master);
        //Assign references
        viewPager = (ViewPager) findViewById(R.id.rootView);
        dots = (LinearLayout) findViewById(R.id.dots);
        btnSkip = (Button) findViewById(R.id.backBtn);
        btnNext = (Button) findViewById(R.id.nextBtn);
        adapter = new ViewPageAdapter();
        //Create references to each of the screens
        myLayouts = new int[]{
                R.layout.welcome_layout_1,R.layout.welcome_layout_2,
                R.layout.welcome_layout_3,R.layout.welcome_layout_4,
                R.layout.welcome_layout_5
        };
        i = new Intent(this, MainActivity.class);
        // Making notification bar transparent, if supported
        if (Build.VERSION.SDK_INT >= 21) {
            getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
        }
        changeStatusBarColor();
        modifyDots(0);
        attachButtonListeners();
        viewPager.setAdapter(adapter);


    }

    private void modifyDots(int currentPage){
        //Get associations
        TextView[] dotsTextViewArray = new TextView[myLayouts.length];
        int[] colorsActive = getResources().getIntArray(R.array.colorActive);
        int[] colorsInactive = getResources().getIntArray(R.array.colorInactive);
        dots.removeAllViews();
        //Remove dots view and then recreate it using the current page
        for(int i=0;i<dotsTextViewArray.length;i++){
            dotsTextViewArray[i]= new TextView(this);
            dotsTextViewArray[i].setText("•"); //Set the text to a dot!
            dotsTextViewArray[i].setTextSize(35);
            dotsTextViewArray[i].setTextColor(colorsInactive[currentPage]);
            dots.addView(dotsTextViewArray[i]);
        }

        if (dotsTextViewArray.length > 0)
            dotsTextViewArray[currentPage].setTextColor(colorsActive[currentPage]);

    }


    //Adds listeners to buttons and the ViewPager.
    private void attachButtonListeners(){
        btnNext.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(getCurrentItem() >= myLayouts.length){//eg. a "Finish" click
                    startActivity(i);
                    finish();
                }else{//We're still scrolling, so let's tell the pager to take us to the next page!
                    viewPager.setCurrentItem(getCurrentItem());
                }
            }
        });
        btnSkip.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                    startActivity(i);
                    finish();
            }
        });
        viewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }

            @Override
            public void onPageSelected(int position) {
                modifyDots(position);
                if(position == myLayouts.length -1){//Last page, let's modify the buttons!
                    btnNext.setText(getString(R.string.ok_btn));
                    btnSkip.setVisibility(View.INVISIBLE);
                }else{//Still some pages left. Let's keep the buttons consistent.
                    btnNext.setText(getString(R.string.next_btn));
                    btnSkip.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });
    }

    private void changeStatusBarColor() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.setStatusBarColor(Color.TRANSPARENT);
        }
    }
    private int getCurrentItem(){
        return viewPager.getCurrentItem() + 1;
    }

    //Most of this explains itself. Basically in charge of changing pages and layouts.
    private class ViewPageAdapter extends PagerAdapter{
        private LayoutInflater layoutInflater;
        @Override
        public Object instantiateItem(ViewGroup container, int position){
            //Get our inflater
            layoutInflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            View view = layoutInflater.inflate(myLayouts[position],container,false);
            container.addView(view);
            return view;
        }

        @Override
        public int getCount(){
            return myLayouts.length;
        }

        @Override
        public boolean isViewFromObject(View view, Object obj) {
            return view == obj;
        }

        @Override
        public void destroyItem(ViewGroup container, int position, Object object){
            View view = (View) object;
            container.removeView(view);
        }
    }
}
