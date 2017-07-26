package com.jk.kangdi.libbutterknife;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.TextView;

import com.jk.kangdi.BindView;

public class MainActivity extends AppCompatActivity {

    @BindView("R.id.tv")
    TextView tv;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }
}
