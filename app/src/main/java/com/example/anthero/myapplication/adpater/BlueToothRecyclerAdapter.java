package com.example.anthero.myapplication.adpater;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.clj.fastble.data.BleDevice;
import com.example.anthero.myapplication.R;
import com.example.anthero.myapplication.activity.MainActivity;
import com.example.anthero.myapplication.activity.UpdateMeshActivity;
import com.example.anthero.myapplication.fragment.LoginDialogFragment;

import java.util.List;

public class BlueToothRecyclerAdapter extends RecyclerView.Adapter<BlueToothRecyclerAdapter.MyHodler> {

    private Context context;
    private List<BleDevice> bleDevices;
    private int type = 0;

    public BlueToothRecyclerAdapter(Context context, List<BleDevice> bleDevices){
        this.context = context;
        this.bleDevices = bleDevices;
        Log.d("size", bleDevices.size()+"");
    }

    public int addDevice(BleDevice device){
        bleDevices.add(device);
        notifyItemInserted(getItemCount()-1);
        Log.d("size", bleDevices.size()+"");
        return getItemCount();
    }

    @NonNull
    @Override
    public MyHodler onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        final View view = LayoutInflater.from(context).inflate(R.layout.list_item, parent, false);
        return new MyHodler(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MyHodler holder, final int position) {
        if(bleDevices.get(position).getName() != null){
            holder.blueTooth.setText(bleDevices.get(position).getName()+" : "+bleDevices.get(position).getMac());
        }else {
            holder.blueTooth.setText("noneName : "+bleDevices.get(position).getMac());
        }
        holder.blueTooth.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                LoginDialogFragment loginDialogFragment = new LoginDialogFragment();
                Bundle bundle = new Bundle();
                bundle.putParcelable("device", bleDevices.get(position));
                bundle.putInt("type", type);
                loginDialogFragment.setArguments(bundle);
                if(type == 1){
                    loginDialogFragment.setDeviceList(bleDevices);
                    log("长度"+bleDevices.size());
                    loginDialogFragment.show(((UpdateMeshActivity)context).getFragmentManager(), "update");
                }else {
                    loginDialogFragment.show(((MainActivity)context).getFragmentManager(), "login");
                }

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

    public void setType(int type) {
        this.type = type;
    }

    @SuppressLint("LongLogTag")
    private void log(String s){
        Log.e("BlueToothRecyclerAdapter", s);
    }
}
