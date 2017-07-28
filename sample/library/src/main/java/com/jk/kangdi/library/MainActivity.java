package com.jk.kangdi.library;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.jk.kangdi.BindView;
import com.jk.kangdi.BindViews;
import com.jk.kangdi.ContentView;
import com.jk.kangdi.OnClick;
import com.jk.kangdi.lib_butter_knife_api.ButterKnife;

@ContentView("R.layout.activity_main")
public class MainActivity extends AppCompatActivity {

    @BindView("R.id.tv")
    TextView tv;

    @BindViews({"R.id.btn1" , "R.id.btn2" , "R.id.btn3" , "R.id.btn4"})
    Button[] btns;


    private static final String TAG = "MainActivity2";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ButterKnife.bind(this);

        Log.i(TAG, "onCreate: " + tv);

        Log.i(TAG, "onCreate: " + btns);

    }

    @OnClick("R.id.btn1")
    public void onClick(View v){

    }

}
