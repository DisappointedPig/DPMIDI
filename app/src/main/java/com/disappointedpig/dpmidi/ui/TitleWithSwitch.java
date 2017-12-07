package com.disappointedpig.dpmidi.ui;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.animation.AlphaAnimation;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;

import com.disappointedpig.dpmidi.R;

import org.greenrobot.eventbus.EventBus;

public class TitleWithSwitch extends LinearLayout {

    private AlphaAnimation brightenAlpha, dimAlpha;
    TextView titleTextView;

    public int switchResourceId;
    Switch viewSwitch;
    Context ctx;
    public boolean shouldDimWhenOff, switchDefaultState;

    public TitleWithSwitch(Context context) {
        super(context);
        ctx = context;
        init(null, 0);
    }

    public TitleWithSwitch(Context context, AttributeSet attrs) {
        super(context, attrs);
        ctx = context;
        init(attrs, 0);
    }

    public TitleWithSwitch(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        ctx = context;
        init(attrs, defStyle);
    }

    public void init(AttributeSet attrs, int defStyle) {

        // setup dim/brighten for greyed out rows
        brightenAlpha = new AlphaAnimation(1.0F,1.0F);
        brightenAlpha.setDuration(0); // Make animation instant
        brightenAlpha.setFillAfter(true); // Tell it to persist after the animation ends
        dimAlpha = new AlphaAnimation(0.5F,0.5F);
        dimAlpha.setFillAfter(true); // Tell it to persist after the animation ends
        dimAlpha.setDuration(0); // Make animation instant

        // Load attributes
        final TypedArray a = getContext().obtainStyledAttributes(
                attrs, R.styleable.TitleWithSwitch, defStyle, 0);

//        String titleText = a.getString(R.styleable.TitleWithSwitch_titleText);
        int titleResId = a.getResourceId(R.styleable.TitleWithSwitch_titleSwitch_Title_ResId,R.string.missing_string_resource);
        switchDefaultState = a.getBoolean(R.styleable.TitleWithSwitch_titleSwitch_Switch_State, false);
        shouldDimWhenOff = a.getBoolean(R.styleable.TitleWithSwitch_titleSwitch_Switch_Dim,true);
        switchResourceId = a.getResourceId(R.styleable.TitleWithSwitch_titleSwitch_Switch_ResId,0);
        a.recycle();

        setGravity(Gravity.CENTER_VERTICAL);

        LayoutInflater inflater = (LayoutInflater) ctx
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.view_switch_with_text, this, true);

        titleTextView = (TextView) getChildAt(0);
        titleTextView.setText(titleResId);

        viewSwitch = (Switch) getChildAt(1);
        viewSwitch.setChecked(getSwitchState());
        handleOnSwitchChanged(getSwitchState());
//        viewSwitch.setTag(null);
        if(!shouldDimWhenOff) {
            titleTextView.startAnimation(brightenAlpha);
            titleTextView.setTypeface(null, Typeface.BOLD);
        }
        viewSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
//                if (viewSwitch.getTag() != null) {
//                    Log.d("TWS","tag not null");
//                    viewSwitch.setTag(null);
//                    return;
//                }
                if(switchResourceId != 0) {
                    Log.d("TWS","send event "+(isChecked ? "ON" : "OFF"));
                    EventBus.getDefault().post(new CustomUIFieldChangedEvent(switchResourceId, isChecked));
                } else {
                    Log.d("TWS","switchResourceId is 0");
                }
                handleOnSwitchChanged(isChecked);

            }
        });
    }

    public boolean getSwitchState() {
        return switchDefaultState;
    }

    public void handleOnSwitchChanged(boolean isChecked) {
        if(shouldDimWhenOff) {
            dimView(!isChecked);
        }
    }

    public void dimView(boolean b) {
        if(b) {
            titleTextView.startAnimation(dimAlpha);
        } else {
            titleTextView.startAnimation(brightenAlpha);
        }
    }

    public void setSwitch(boolean b) {
        if(viewSwitch.isChecked() && b) { return; }
//        viewSwitch.setTag("TAG");
        viewSwitch.setChecked(b);
//        handleOnSwitchChanged(b);
    }

    public void setShouldDimWhenOff(boolean b) {
        shouldDimWhenOff = b;
    }
}
