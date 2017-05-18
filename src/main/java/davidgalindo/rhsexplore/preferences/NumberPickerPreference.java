package davidgalindo.rhsexplore.preferences;

import android.content.Context;
import android.content.res.TypedArray;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.NumberPicker;

/**
 * Created by David on 5/17/2017.
 */

public class NumberPickerPreference extends DialogPreference {
    public static final int MAX = 15;
    public static final int MIN = 0;
    public static final int DEFAULT = 5;

    private NumberPicker numberPicker;
    private int value;

    public NumberPickerPreference(Context context, AttributeSet attributeSet){
        super(context,attributeSet);
    }

    @Override
    protected Object onGetDefaultValue(TypedArray array, int index){
        return array.getInt(index,DEFAULT);
    }

    @Override
    protected View onCreateDialogView(){
        FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        layoutParams.gravity = Gravity.CENTER;

        numberPicker = new NumberPicker(getContext());
        numberPicker.setLayoutParams(layoutParams);

        FrameLayout dialogView = new FrameLayout(getContext());
        dialogView.addView(numberPicker);

        return dialogView;
    }

    @Override
    protected void onBindDialogView(View view) {
        super.onBindDialogView(view);
        numberPicker.setMinValue(MIN);
        numberPicker.setMaxValue(MAX);
        numberPicker.setValue(value);
    }

    @Override
    protected void onDialogClosed(boolean positiveResult) {
        if (positiveResult) {
            numberPicker.clearFocus();
            int newValue = numberPicker.getValue();
            if (callChangeListener(newValue)) {
                setValue(newValue);
            }
        }
    }

    @Override
    protected void onSetInitialValue(boolean restorePersistedValue, Object defaultValue){
        if(restorePersistedValue){//eg. we want the persisted value restored
            setValue(getPersistedInt(DEFAULT));
        }else{//We can't so we set a default value
            setValue((int)defaultValue);
        }
    }

    public void setValue(int _value){
        //Set the value and then persist it
        value = _value;
        persistInt(value);
    }

    public int getValue(){
        return value;
    }
}
