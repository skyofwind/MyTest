package com.antheroiot.utils;

import android.util.Log;

import com.antheroiot.utils.beacon.Beacon;
import com.antheroiot.utils.beacon.BeaconItem;

/**
 * Created by ruifen9 on 2017/2/24 at antheroiot
 */

public class MeshBeacon {
    public int pid;//0x4001~0x4321
    public int companyId;//0x0211
    public int meshAddress;
    public String deviceName = "";
    public String mac = "";
    public int rssi;
    public byte[] advertisementData;
    
    public void load(String mac, String name, int rssi, byte[] advertisementData) {
        if (advertisementData == null || advertisementData.length == 0) {
            return;
        }
        this.mac = mac;
        this.deviceName = name;
        this.rssi = rssi;
        this.advertisementData = advertisementData;
        Beacon beacon = new Beacon(advertisementData);
        for (BeaconItem beaconItem : beacon.mItems) {
            if (beaconItem.type == 0x09) {
                deviceName = new String(beaconItem.bytes);
            }
            if (beaconItem.type == 0xff && beaconItem.bytes.length >= 13) {
                byte[] bytes = beaconItem.bytes;
                companyId = bytes[1] << 8 | bytes[0] & 0xff;
                if (isMeshDevice()) {
                    pid = bytes[9] << 8 | bytes[8];
                    meshAddress = bytes[12] << 8 | bytes[11] & 0xff;
                }
            }
        }
        Log.d("MeshBeacon", this.toString());
    }

    public boolean isMeshDevice() {
        return companyId == 0x0211;
    }

    public boolean isEncryptDevice() {
        return (pid & 0x4000) == 0x4000;
    }

    @Override
    public String toString() {
        return "MeshBeacon{" +
                "pid=" + pid +
                ", companyId=" + companyId +
                ", meshAddress=" + meshAddress +
                ", deviceName='" + deviceName + '\'' +
                ", mac='" + mac + '\'' +
                ", rssi=" + rssi +
                '}';
    }

}
