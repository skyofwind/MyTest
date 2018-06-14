package com.example.anthero.myapplication;


public class DeviceStatus {
    static final int ALL = -1;

    private int meshAddress;//地址
    private boolean isOnline;//在线状态
    private int status;//状态
    private int pid;//id

    public DeviceStatus(int meshAddress, int seq, int status, int pid) {
        this.meshAddress = meshAddress;
        this.isOnline = seq > 0;
        this.status = status;
        this.pid = pid;
    }

    public int getMeshAddress() {
        if (meshAddress == ALL) {
            return 0xffff;
        }
        return meshAddress;
    }

    public boolean isOnline() {
        return isOnline;
    }

    /**
     * @return hasChanged
     */
    public boolean setOnline(int seq) {
        if (isOnline != (seq > 0)) {
            isOnline = seq > 0;
            return true;
        }
        return false;
    }

    public int getStatus() {
        return status;
    }

    /**
     * @return hasChanged
     */
    public boolean setStatus(int status) {
        if (this.status != status) {
            this.status = status;
            return true;
        }
        return false;
    }

    public int getPid() {
        return pid;
    }

    /**
     * @return hasChanged
     */
    public boolean setPid(int pid) {
        if (this.pid != pid) {
            this.pid = pid;
            return true;
        }
        return false;
    }

    public boolean isChanged(DeviceStatus deviceStatus) {
        return (deviceStatus.getStatus() == 0 && status != 0) || (deviceStatus.getStatus() != 0 && status == 0) || isOnline != deviceStatus.isOnline;
    }
}
