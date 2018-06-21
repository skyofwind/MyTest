package com.example.anthero.myapplication.activity;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;

import com.example.anthero.myapplication.R;
import com.example.anthero.myapplication.adpater.HistoryRecyclerAdapter;
import com.example.anthero.myapplication.database.BleDBDao;
import com.example.anthero.myapplication.decoration.DividerItemDecoration;
import com.example.anthero.myapplication.modle.MyBleDevice;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

public class HistoryActvity extends BaseActvity {

    @BindView(R.id.history_list)
    RecyclerView historyList;
    private List<MyBleDevice> myBleDevices;
    private HistoryRecyclerAdapter adapter;

    @SuppressLint("HandlerLeak")
    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message message) {
            switch (message.what) {
                case 0x01:
                    String text = message.obj.toString();
                    statrProgressDialog(text);
                    break;
                case 0x02:
                    cancel();
                    break;
            }
        }
    };

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.history_layout);
        ButterKnife.bind(this);
        init();
    }

    private void getData() {
        myBleDevices = BleDBDao.getInstance(this).getAllData();
        if(myBleDevices == null){
            myBleDevices = new ArrayList<>();
        }
    }

    private void init() {
        getData();
        initRecycler();
    }

    private void initRecycler(){
        adapter = new HistoryRecyclerAdapter(this, myBleDevices);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        historyList.setLayoutManager(layoutManager);
        historyList.setAdapter(adapter);
        historyList.addItemDecoration(new DividerItemDecoration(this, DividerItemDecoration.VERTICAL_LIST));
    }
}
