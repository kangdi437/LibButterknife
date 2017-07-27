package com.jk.kangdi.libbutterknife;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.jk.kangdi.BindView;
import com.jk.kangdi.BindViews;
import com.jk.kangdi.ContentView;
import com.jk.kangdi.lib_butter_knife_api.ButterKnife;

@ContentView("R.layout.activity_main")
public class MainActivity extends AppCompatActivity {

    @BindView("R.id.tv")
    TextView tv;

    @BindViews({"R.id.btn1" , "R.id.btn2" , "R.id.btn3" , "R.id.btn4"})
    Button[] btns;


    private static final String TAG = "MainActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ButterKnife.bind(this);
        tv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(MainActivity.this , com.jk.kangdi.library.MainActivity.class));
            }
        });


        btns[0].setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.i(TAG, "onClick: " + com.jk.kangdi.libbutterknife.R.layout.activity_main);
                Log.i(TAG, "onClick: " + com.jk.kangdi.library.R.layout.activity_main);
            }
        });
        Log.i(TAG, "onCreate: " + tv);

        Log.i(TAG, "onCreate: " + btns);

    }
}
