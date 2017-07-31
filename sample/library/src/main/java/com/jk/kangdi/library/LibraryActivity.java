package com.jk.kangdi.library;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.TextView;

import com.chad.library.adapter.base.BaseQuickAdapter;
import com.chad.library.adapter.base.BaseViewHolder;
import com.jk.kangdi.BindView;
import com.jk.kangdi.ContentView;
import com.jk.kangdi.OnClick;
import com.jk.kangdi.internal.Convert;
import com.jk.kangdi.internal.QuickAdapter;


@ContentView("R.layout.activity_library")
public class LibraryActivity extends AppCompatActivity {


    @BindView("R.id.as")
    TextView as;

    @QuickAdapter("R.layout.item")
    BaseQuickAdapter<String , BaseViewHolder> adapter;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_library);
    }

    @OnClick("R.id.as")
    public void onClick(View view){

    }

    @Convert
    public void convert(){

    }

}
