package davidgalindo.rhsexplore;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.Toast;

/**
 * Created by David on 5/8/2017.
 */

public class AboutActivity extends AppCompatActivity {
    private Toolbar t;
    @Override
    public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.about);
        t = (Toolbar) findViewById(R.id.toolBar);
        setSupportActionBar(t);
        setTitle("Credits");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);

    }

    public void itsASecretToEverybody(View view){
        Toast.makeText(getApplicationContext(),"I'm Big Sean, and I like to party",Toast.LENGTH_LONG).show();
    }
}
