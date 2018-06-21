package com.example.anthero.myapplication.adpater;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.antheroiot.mesh.CommandFactory;
import com.antheroiot.mesh.MeshProtocol;
import com.clj.fastble.data.BleDevice;
import com.example.anthero.myapplication.R;
import com.example.anthero.myapplication.activity.BleDeviceControlActivity;
import com.example.anthero.myapplication.modle.DeviceStatus;
import com.example.anthero.myapplication.reciver.BleControlReciver;
import com.example.anthero.myapplication.utils.Config;
import com.example.anthero.myapplication.utils.ControlUtil;
import com.example.anthero.myapplication.utils.SystemUtil;

import java.util.List;

public class DeviceManagerRecyclerAdapter extends RecyclerView.Adapter<DeviceManagerRecyclerAdapter.MyHodler> {

    private SparseArray<DeviceStatus> deviceStatuses;
    private List<Integer> index;
    private Context context;
    private BleDevice bleDevice;


    public DeviceManagerRecyclerAdapter(Context context, SparseArray<DeviceStatus> deviceStatuses, List<Integer> index, BleDevice bleDevice) {
        this.context = context;
        this.deviceStatuses = deviceStatuses;
        this.index = index;
        this.bleDevice = bleDevice;
    }

    @NonNull
    @Override
    public MyHodler onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        final View view = LayoutInflater.from(context).inflate(R.layout.device_manager_item, parent, false);
        return new MyHodler(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MyHodler holder, int position) {
        DeviceStatus deviceStatus = deviceStatuses.get(index.get(position));
        holder.device.setText(deviceStatus.getMeshAddress() + "");
        if (deviceStatus.getStatus() == 0) {
            holder.background.setBackgroundColor(SystemUtil.getColor(context, R.color.white));
        } else {
            holder.background.setBackgroundColor(SystemUtil.getColor(context, R.color.light));
        }
        if (deviceStatus.isOnline()) {
            holder.online.setText(R.string.online);
        } else {
            holder.online.setText(R.string.offline);
            holder.background.setBackgroundColor(SystemUtil.getColor(context, R.color.white));
        }

    }

    @Override
    public int getItemCount() {
        return deviceStatuses.size();

    }

    class MyHodler extends RecyclerView.ViewHolder {

        TextView device, online;
        LinearLayout background;

        public MyHodler(View itemView) {
            super(itemView);
            device = (TextView) itemView.findViewById(R.id.device);
            online = (TextView) itemView.findViewById(R.id.online);
            background = (LinearLayout) itemView.findViewById(R.id.background);

            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    DeviceStatus status = deviceStatuses.get(index.get(getAdapterPosition()));
                    boolean onoff = false;
                    if(status.getStatus() == 0){
                        onoff = true;
                    }else {
                        onoff = false;
                    }
                    if(Config.controlType){
                        log("群组点击 = "+onoff+" "+status.getStatus());
                        try {
                            Thread.sleep(100);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        ControlUtil.getInstance().power(bleDevice, Config.groupId, onoff);
                    }else {
                        ControlUtil.getInstance().power(bleDevice, status.getMeshAddress(), onoff);
                    }

                }
            });
            itemView.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View view) {
                    DeviceStatus status = deviceStatuses.get(index.get(getAdapterPosition()));
                    Intent intent = new Intent(context, BleDeviceControlActivity.class);
                    Bundle bundle = new Bundle();
                    bundle.putParcelable(BleControlReciver.DEVICE, bleDevice);
                    intent.putExtra(BleControlReciver.MESH_ADDRESS, status.getMeshAddress());
                    intent.putExtras(bundle);
                    context.startActivity(intent);
                    return true;
                }
            });
        }
    }


    public void addDeviceStatus(DeviceStatus deviceStatus, boolean first) {
        if (deviceStatuses != null) {
            int p = index.indexOf(deviceStatus.getMeshAddress());
            log("p = "+p);
            if (first) {
                deviceStatuses.put(deviceStatus.getMeshAddress(), deviceStatus);
                if(Config.controlType){
                    ControlUtil.getInstance().write(bleDevice,
                            MeshProtocol.SERVICE_MESH.toString(),
                            MeshProtocol.CHARA_COMMAND.toString(),
                            CommandFactory.setGroupId(deviceStatus.getMeshAddress(), true, Config.groupId).getData(true));

                }
                notifyItemInserted(p);
            } else {
                deviceStatuses.put(deviceStatus.getMeshAddress(), deviceStatus);
                notifyItemChanged(p);
            }

        }
    }

    public void addIndex(int address) {
        index.add(address);
        log(getItemCount() + "");
    }

    public void setBleDevice(BleDevice bleDevice) {
        this.bleDevice = bleDevice;
    }

    public boolean isChange(DeviceStatus deviceStatus){
        DeviceStatus last = deviceStatuses.get(deviceStatus.getMeshAddress());
        if(last.getStatus() != deviceStatus.getStatus() || last.isOnline() != deviceStatus.isOnline()){//status或online发生改变时确定变化
            return true;
        }
        return false;
    }

    @SuppressLint("LongLogTag")
    private void log(String s) {
        Log.d("DeviceManagerRecyclerAdapter", s);
    }

    public void setGroup(boolean a){
        for(int i = 0; i < index.size(); ++i){
            ControlUtil.getInstance().write(bleDevice,
                    MeshProtocol.SERVICE_MESH.toString(),
                    MeshProtocol.CHARA_COMMAND.toString(),
                    CommandFactory.setGroupId(deviceStatuses.get(index.get(i)).getMeshAddress(), a, Config.groupId).getData(true)
            );
        }
    }
}
