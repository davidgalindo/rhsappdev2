package davidgalindo.rhsexplore;

import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;

/**
 * Created by Sean on 5/4/2017.
 */
public class House {

    private String name, yearBuilt;
    private Drawable imageId;
    private String imageURL;
    private String houseURL;


    public House(String picURL, String _name, String _yearBuilt, String house)
    {
        imageURL = picURL;
        name = _name;
        yearBuilt = _yearBuilt;
        houseURL = house;
    }


    @Override
    public String toString(){
        return name + " " + yearBuilt + " " + imageURL;
    }

    public String getName() {
        return name;
    }

    public String getYearBuilt() {
        return yearBuilt;
    }

    public String getImageURL(){return imageURL;}
    public String getHouseURL(){return houseURL;}
}