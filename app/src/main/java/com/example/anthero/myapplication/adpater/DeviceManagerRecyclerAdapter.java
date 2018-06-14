package com.example.anthero.myapplication;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.List;

public class DeviceManagerRecyclerAdapter extends RecyclerView.Adapter<DeviceManagerRecyclerAdapter.MyHodler> {

    private List<DeviceStatus> deviceStatuses;
    private Context context;

    public DeviceManagerRecyclerAdapter(Context context, List<DeviceStatus> deviceStatuses){
        this.context = context;
        this.deviceStatuses = deviceStatuses;
    }

    @NonNull
    @Override
    public MyHodler onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        final View view = LayoutInflater.from(context).inflate(R.layout.list_item, parent, false);
        return new MyHodler(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MyHodler holder, int position) {
//        BleDevice bleDevice = bleDevices.get(position);
//        if(bleDevice.getName() != null){
//            holder.device.setText(bleDevice.getName());
//        }else {
//            holder.device.setText("none");
//        }

    }

    @Override
    public int getItemCount() {
        return deviceStatuses.size();
    }

    class MyHodler extends RecyclerView.ViewHolder{

        TextView device;

        public MyHodler(View itemView) {
            super(itemView);
            device = (TextView)itemView.findViewById(R.id.device);
        }
    }
}
