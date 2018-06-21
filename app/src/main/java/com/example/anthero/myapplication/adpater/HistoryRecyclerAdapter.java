package com.example.anthero.myapplication.adpater;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.antheroiot.mesh.MeshProtocol;
import com.clj.fastble.data.BleDevice;
import com.example.anthero.myapplication.R;
import com.example.anthero.myapplication.activity.DeviceManagerActivity;
import com.example.anthero.myapplication.modle.MyBleDevice;

import java.util.List;

public class HistoryRecyclerAdapter extends RecyclerView.Adapter<HistoryRecyclerAdapter.MyHodler> {

    private Context context;
    private List<MyBleDevice> bleDevices;

    public HistoryRecyclerAdapter(Context context, List<MyBleDevice> bleDevices){
        this.context = context;
        this.bleDevices = bleDevices;
        Log.d("size", bleDevices.size()+"");
    }

    public int addDevice(MyBleDevice device){
        bleDevices.add(device);
        notifyItemInserted(getItemCount()-1);
        Log.d("size", bleDevices.size()+"");
        return getItemCount();
    }

    @NonNull
    @Override
    public HistoryRecyclerAdapter.MyHodler onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        final View view = LayoutInflater.from(context).inflate(R.layout.list_item, parent, false);
        return new MyHodler(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MyHodler holder, final int position) {
        final BleDevice device = bleDevices.get(position).getBleDevice();
        if(device.getName() != null){
            holder.blueTooth.setText(device.getName()+" : "+device.getMac());
        }else {
            holder.blueTooth.setText("noneName : "+device.getMac());
        }
        holder.blueTooth.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                MeshProtocol.getInstance().preLogin(device.getMac(), device.getName(), bleDevices.get(position).getPassword());
                Intent intent = new Intent(context, DeviceManagerActivity.class);
                Bundle bundle = new Bundle();
                bundle.putParcelable("device", device);
                intent.putExtras(bundle);
                context.startActivity(intent);
            }
        });
    }

    @Override
    public int getItemCount() {

        return bleDevices.size();

    }

    class MyHodler extends RecyclerView.ViewHolder{

        TextView blueTooth;

        public MyHodler(View itemView) {
            super(itemView);
            blueTooth = (TextView)itemView.findViewById(R.id.blue_tooth);
        }
    }
}
