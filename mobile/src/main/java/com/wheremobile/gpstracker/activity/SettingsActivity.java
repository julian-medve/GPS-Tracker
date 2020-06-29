package com.wheremobile.gpstracker.activity;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.View;
import android.widget.ImageButton;

import com.wheremobile.gpstracker.R;
import com.wheremobile.gpstracker.fragment.SettingsFragment;

public class SettingsActivity extends BaseActivity implements View.OnClickListener {

    @Override
    protected int getLayout() {
        return R.layout.activity_settings;
    }

    @Override
    protected int getFragmentId() {
        return R.id.content_frame;
    }

    ImageButton ibBack;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addFragment(SettingsFragment.newInstance());
        ibBack = findViewById(R.id.ibBack);
        ibBack.setOnClickListener(this);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.ibBack:
                onBackPressed();
                break;
        }
    }
}
