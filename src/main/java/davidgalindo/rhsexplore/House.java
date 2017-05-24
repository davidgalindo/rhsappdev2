package davidgalindo.rhsexplore;

import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;

/**
 * Created by Sean on 5/4/2017.
 */
public class House {

    private String name, yearBuilt;
    private String imageURL;
    private String houseURL;
    private String address;


    public House(String picURL, String _name, String _yearBuilt, String house, String _address)
    {
        imageURL = picURL;
        name = _name;
        yearBuilt = _yearBuilt;
        houseURL = house;
        address = _address;
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

    public String getAddress() {
        return address;
    }
}