package com.example.anthero.myapplication;

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

import com.clj.fastble.data.BleDevice;

import java.util.List;

public class BlueToothRecyclerAdapter extends RecyclerView.Adapter<BlueToothRecyclerAdapter.MyHodler> {

    private Context context;
    private List<BleDevice> bleDevices;
    private View.OnClickListener backTop;

    public BlueToothRecyclerAdapter(Context context, List<BleDevice> bleDevices){
        this.context = context;
        this.bleDevices = bleDevices;
        Log.d("size", bleDevices.size()+"");
    }

    public int addDevice(BleDevice device){
        bleDevices.add(device);
        notifyItemInserted(getItemCount()-2);
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
        if(position == getItemCount()-1){
            holder.blueTooth.setText("点击回到顶部");
            holder.blueTooth.setOnClickListener(backTop);
        }else {
            if(bleDevices.get(position).getName() != null){
                holder.blueTooth.setText(bleDevices.get(position).getName()+" : "+bleDevices.get(position).getMac());
            }else {
                holder.blueTooth.setText("noneName : "+bleDevices.get(position).getMac());
            }
            holder.blueTooth.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Intent intent = new Intent(context, LoginActvity.class);
                    Bundle bundle = new Bundle();
                    bundle.putParcelable("device", bleDevices.get(position));
                    intent.putExtras(bundle);
                    context.startActivity(intent);
                }
            });
        }

    }

    @Override
    public int getItemCount() {
        if(bleDevices.size() == 0){
            return 0;
        }else {
            return bleDevices.size()+1;
        }
    }

    class MyHodler extends RecyclerView.ViewHolder{

        TextView blueTooth;

        public MyHodler(View itemView) {
            super(itemView);
            blueTooth = (TextView)itemView.findViewById(R.id.blue_tooth);
        }
    }

    public void setBackTop(View.OnClickListener clickListener){
        backTop = clickListener;
    }
}
