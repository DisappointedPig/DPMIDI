package com.disappointedpig.dpmidi.ui;

import android.content.Context;
import android.content.res.TypedArray;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.disappointedpig.dpmidi.R;

import org.greenrobot.eventbus.EventBus;

public class TitleWithEditText extends RelativeLayout {

    Context ctx;

    EditText editText;
    int typeReourceId;
    boolean reportUpdate = true;

    public TitleWithEditText(Context context) {
        super(context);
        this.ctx = context;
        init(null, 0);
    }

    public TitleWithEditText(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.ctx = context;
        init(attrs, 0);
    }

    public TitleWithEditText(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        this.ctx = context;
        init(attrs, defStyle);
    }

    private void init(AttributeSet attrs, int defStyle) {
        // Load attributes
        final TypedArray a = getContext().obtainStyledAttributes(
                attrs, R.styleable.TitleWithEditText, defStyle, 0);

        int titleResourceId = a.getResourceId(
                R.styleable.TitleWithEditText_titleWithEdit_Title_ResId,R.string.missing_string_resource);
        int hintResourceId = a.getResourceId(
                R.styleable.TitleWithEditText_titleWithEdit_Hint_ResId,R.string.missing_string_resource);
        typeReourceId = a.getResourceId(R.styleable.TitleWithEditText_titleWithEdit_Type_ResId,0);

        a.recycle();

        LayoutInflater inflater = (LayoutInflater) ctx
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.view_static_title_with_edit_text, this, true);

        RelativeLayout ll = (RelativeLayout) getChildAt(0);
        TextView one = (TextView) ll.getChildAt(0);
        one.setText(titleResourceId);
        editText = (EditText) ll.getChildAt(1);
        editText.setHint(hintResourceId);
        editText.setSelected(false);

        editText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if(reportUpdate) {
                    handleTextChanged();
                } else {
                    reportUpdate = true;
                }
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });
    }

    public String getEditText() {
        return editText.getText().toString();
    }

    public void setEditText(String s) {
        reportUpdate = false;
        editText.setText(s);
        editText.setSelection(editText.getText().length());
    }

    public void handleTextChanged() {
        EventBus.getDefault().post(new CustomUIFieldChangedEvent(typeReourceId,getEditText()));
    }

}