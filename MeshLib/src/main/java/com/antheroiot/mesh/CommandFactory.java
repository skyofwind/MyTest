package com.antheroiot.mesh;

import android.graphics.Color;
import android.util.Log;

import com.telink.util.ArraysUtils;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Random;

/**
 * 推荐使用蓝牙库 : <a href="http://polidea.github.io/RxAndroidBle">RxAndroidBle</a>
 *
 * @author cg
 * <p>
 * meshAddress 0
 * 1~0x7fff 具体某个设备
 * 0x8000~0xfffe group 指定分组地址群
 * 0xffff 所有name以及密码相同的设备
 */

public class CommandFactory {

    public final static int TIMER_REPEAT_MODE_ONCE = 7;
    public final static int TIMER_REPEAT_MODE_EVERYDAY = 1;
    public final static int TIMER_REPEAT_MODE_WEEKDAY = 2;


    ///////////////////////////////////////////////////////////////////////////
    // Mesh通用接口
    ///////////////////////////////////////////////////////////////////////////

    /**
     * 获取分组下所有设备的详细信息
     */
    public static CommonData getDeviceOfMesh(int targetAddress) {
        byte[] bytes = {10};
        return packetData(targetAddress, bytes, Opcode.REQUEST_DEVICE_DETAIL);
    }

    /**
     * 用于修改设备的MeshAddress,固件不会重启
     *
     * @param oldMeshAddress 旧的MeshAddress
     * @param newMeshAddress 新的MeshAddress
     * @return CommonData
     */
    public static CommonData changeMeshAddress(int oldMeshAddress, int newMeshAddress) {
        int addrLo = newMeshAddress & 0xff;
        int addrHi = (newMeshAddress >> 8) & 0xff;
        byte[] params = {(byte) 0x0a, (byte) addrLo, (byte) addrHi};
        return packetData(oldMeshAddress, params);
    }

    /**
     * 恢复出厂设置
     */
    public static CommonData reset(int targetAddress) {
        byte[] params = {(byte) 0xc0, 0};
        return packetData(targetAddress, params);
    }

    /**
     * 设置分组
     */
    public static CommonData setGroupId(int targetAddress, boolean isSet, int groupId) {
        //0:del;1:set
        int op = isSet ? 1 : 0;
        final int cmdByte = 0x09;
        int grpLow = groupId & 0xff;
        int grpHigh = (groupId >> 8) & 0xff;
        byte[] params = {(byte) cmdByte, (byte) op, (byte) grpLow, (byte) grpHigh};
        Log.d("myGrouptest", "setGrounpId已经设置了" + targetAddress + " " + isSet + " " + groupId);
        return packetData(targetAddress, params);
    }

    /**
     * 更新设备时间
     */
    public static CommonData updateDeviceTime(int targetAddress) {
        Calendar calendar = Calendar.getInstance();  //获取当前时间，作为图标的名字
        int year = calendar.get(Calendar.YEAR) - 2000;
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        int minute = calendar.get(Calendar.MINUTE);
        int second = 0;
        byte[] params = {2, (byte) year, (byte) month, (byte) day, (byte) hour, (byte) minute, (byte) second};
        return packetData(targetAddress, params);
    }

    /**
     * 删除场景
     */
    public static CommonData delScene(int targetAddress, int sceneId) {
        byte[] params = {(byte) 0x06, (byte) 0, (byte) sceneId};
        return packetData(targetAddress, params);
    }

    /**
     * 启动场景
     */
    public static CommonData startScene(int targetAddress, int sceneId) {
        byte[] params = {(byte) 0x06, (byte) 2, (byte) sceneId};
        return packetData(targetAddress, params);
    }

    /**
     * 停止场景
     */
    public static CommonData stopScene(int targetAddress, int sceneId) {
        byte[] params = {(byte) 0x06, (byte) 3, (byte) sceneId};
        return packetData(targetAddress, params);
    }

    /**
     * 获取指定设备的场景
     *
     * @param sceneId 0x00获取设备所有场景详情 ,0xFF:获取已经使用的场景详情 , 0x01~0x10:获取设备对应的场景信息;
     *                <p>
     *                response:1~3byte[seq] 4~5[src] 6~7[dst] 8[Opcode.RESPONSE_SCENE] 9[0x11] 10[0x02] 11[Scene id] 12~18[对应配置的场景命令] 19[该设备有多少个场景] 20[0]
     *                </p>
     */
    public static CommonData getScene(int targetAddress, int sceneId) {
        byte[] params = {(byte) 0x10, (byte) sceneId};
        return packetData(targetAddress, params, Opcode.REQUEST_SCENE);
    }


    ///////////////////////////////////////////////////////////////////////////
    // 灯类
    ///////////////////////////////////////////////////////////////////////////

    /**
     * 开关
     */
    public static CommonData power(int targetAddress, boolean onoff) {
        byte[] params = {0, (byte) (onoff ? 1 : 0)};
        return packetData(targetAddress, params);
    }

    /**
     * limit y+w<=255
     *
     * @param y 暖白光的亮度 [0~255]
     * @param w 冷白光的亮度 [0~255]
     */
    private static CommonData _setLight(int targetAddress, int y, int w) {
        if (y == 0 && w == 0) {
            y = 1;
            w = 1;
        }
        byte[] params = {1, 1, (byte) y, (byte) w, 0, 0, 0};
        return packetData(targetAddress, params);
    }

    /**
     * 白光的色温调节
     *
     * @param cctRate    [0,1]
     * @param brightness [1,255]
     */
    public static CommonData setLight(int targetAddress, float cctRate, int brightness) {
        if (brightness < 1) brightness = 1;
        if (brightness > 255) brightness = 255;
        int y, w;
        w = (int) (cctRate * brightness);
        y = (int) ((1 - cctRate) * brightness);
        return _setLight(targetAddress, y, w);
    }

    /**
     * 颜色的调节
     */
    public static CommonData setColor(int targetAddress, int color) {
        int r, g, b;
        try {
            r = Color.red(color);
            g = Color.green(color);
            b = Color.blue(color);
        } catch (Exception e) {
            r = 1;
            g = 1;
            b = 1;
        }
        if (r == 0 && g == 0 && b == 0) {
            r = 1;
            g = 1;
            b = 1;
        }
        byte[] params = {1, 1, 0, 0, (byte) r, (byte) g, (byte) b};
        return packetData(targetAddress, params);
    }


    public static final int FUNC_STOP_EFFECT = 0;
    public static final int FUNC_FLASH = 1;
    public static final int FUNC_BREATH = 2;
    public static final int HZ_FAST = 2;
    public static final int HZ_MID = 4;
    public static final int HZ_SLOW = 8;

    /**
     * 设置灯光效果: 灯光闪烁,灯光呼吸
     *
     * @param func      {@link #FUNC_STOP_EFFECT} 停止灯光效果
     *                  {@link #FUNC_FLASH} 闪烁效果
     *                  {@link #FUNC_BREATH} 呼吸效果
     * @param rgbwc     bit0:Red
     *                  bit1:Green
     *                  bit2:Blue
     *                  bit3:w
     *                  bit4:y
     * @param hz        {@link #HZ_FAST} 快
     *                  {@link #HZ_MID} 中
     *                  {@link #HZ_SLOW} 慢
     * @param flashTime 闪灯功能时,闪烁的次数[0~255],呼吸功能暂无作用
     * @since v6.0.0以后版本有效
     */
    public static CommonData setLightEffect(int targetAddress, int func, int rgbwc, int hz, int flashTime) {
        byte[] params = {(byte) 0x0D, (byte) func, (byte) rgbwc, (byte) hz, (byte) flashTime};
        return packetData(targetAddress, params);
    }

    /**
     * 设置流光模式
     *
     * @since v5.4.0以后版本有效
     */
    public static CommonData setStreamModeStart(int targetAddress, int color1, int color2) {
        byte[] params1 = {7, (byte) Color.red(color1), (byte) Color.green(color1), (byte) Color.blue(color1), (byte) Color.red(color2), (byte) Color.green(color2), (byte) Color.blue(color2), (byte) 0xe1};
        return packetData(targetAddress, params1);
    }

    /**
     * 设置流光模式
     *
     * @since v5.4.0以后版本有效
     */
    public static CommonData setStreamModeEnd(int targetAddress, int color3, int color4, int fadeTime) {
        byte[] params2 = {7, (byte) Color.red(color3), (byte) Color.green(color3), (byte) Color.blue(color3), (byte) Color.red(color4), (byte) Color.green(color4), (byte) Color.blue(color4), (byte) fadeTime, (byte) 0xe2};
        return packetData(targetAddress, params2);
    }

    /**
     * 设置流光的渐变时间
     */
    public static CommonData setStreamModeFadeTime(int targetAddress, int fadeTime) {
        byte[] params2 = {7, 0, 0, 0, 0, 0, 0, (byte) fadeTime, (byte) 0xe3};
        return packetData(targetAddress, params2);
    }

    /**
     * 启动流光模式
     */
    public static CommonData startStreamMode(int targetAddress) {
        byte[] params2 = {7, 0, 0, 0, 0, 0, 0, 0, (byte) 0xe4};
        return packetData(targetAddress, params2);
    }

    /**
     * 设置流光模式
     *
     * @since v6.0.0以后版本有效
     */
    public static CommonData setStreamModeEndNoOpen(int targetAddress, int color3, int color4) {
        byte[] params2 = {7, (byte) Color.red(color3), (byte) Color.green(color3), (byte) Color.blue(color3), (byte) Color.red(color4), (byte) Color.green(color4), (byte) Color.blue(color4), (byte) 4, (byte) 0xe5};
        return packetData(targetAddress, params2);
    }

    /**
     * @param repeat 在v6.0.0之前的固件无效
     * @deprecated
     */
    public static CommonData setTimer(int targetAddress, int timerId, boolean enable, int hour, int min, int repeat, boolean isState) {
        int mode = 1;//周重复
        if (repeat == 127) {
            mode = 1;//每天重复
        } else if (repeat == 0) {
            mode = 7;//单次，如果时间比现在早则设置明天的
        }
        if (isState) {
            mode += 128;
        }
        if (enable) {
            timerId += 128;
        }
        final int CMD_SET_TIMER = 3;
        byte[] params = {(byte) CMD_SET_TIMER, (byte) timerId, (byte) mode, (byte) repeat, (byte) hour, (byte) min, (byte) 0, (byte) 120, (byte) 120};
        return packetData(targetAddress, params);
    }
    // TODO: 2017/10/26 新的timer接口

    /**
     * 0 开关
     */
    public static CommonData addScene(int targetAddress, int sceneId, int delay, int y, int w, int r, int g, int b) {
        byte[] params = {(byte) 0x06, (byte) 1, (byte) sceneId, 0, (byte) delay, (byte) y, (byte) w, (byte) r, (byte) g, (byte) b};
        return packetData(targetAddress, params);
    }

    /**
     * 1 闪烁
     *
     * @param times 闪烁次数[0,255] 0:无限次闪烁
     * @param mask  :
     *              bit0:Red
     *              bit1:Green
     *              bit2:Blue
     *              bit3:w
     *              bit4:y
     * @param hz    {@link #HZ_FAST} 快
     *              {@link #HZ_MID} 中
     *              {@link #HZ_SLOW} 慢
     */
    public static CommonData addScene(int targetAddress, int sceneId, int delay, int mask, int hz, int times) {
        byte[] params = {(byte) 0x06, (byte) 1, (byte) sceneId, (byte) 1, (byte) delay, (byte) mask, (byte) hz, (byte) times};
        return packetData(targetAddress, params);
    }

    /**
     * 2 流光
     */
    public static CommonData addScene(int targetAddress, int sceneId, int delay, int speed) {
        Random random = new Random();
        //random 固件用来做对比的数据,与上一次设置的不一样才会使设置生效
        byte[] params = {(byte) 0x06, (byte) 1, (byte) sceneId, (byte) 2, (byte) delay, (byte) speed, (byte) random.nextInt(255), (byte) random.nextInt(255)};
        return packetData(targetAddress, params);
    }

    /**
     * 3 呼吸
     *
     * @param mask  :1byte 0bit~4bit 分别代表R,G,B,W,C是否闪烁
     * @param speed 呼吸变化速度[0,255] 单位秒
     */
    public static CommonData addScene(int targetAddress, int sceneId, int delay, int mask, int speed) {
        byte[] params = {(byte) 0x06, (byte) 1, (byte) sceneId, (byte) 3, (byte) delay, (byte) mask, (byte) speed};
        return packetData(targetAddress, params);
    }


    ///////////////////////////////////////////////////////////////////////////
    // sensor
    ///////////////////////////////////////////////////////////////////////////

    /**
     * 传感器通用
     * 停止/开启 传感器功能
     *
     * @param meshAddress 单播
     * @param pid         传感器pid
     * @param enable      enable/disable
     */
    public static CommonData enableSensor(int meshAddress, int pid, boolean enable) {
        int pidLow = pid & 0xff;
        int pidHigh = (pid >> 8) & 0xff;
        byte[] params = new byte[]{0x11, (byte) pidLow, (byte) pidHigh, 0, (byte) (enable ? 1 : 0)};
        return packetData(meshAddress, params);
    }

    /**
     * 传感器通用
     * 开关传感器指示灯
     *
     * @param meshAddress 单播
     * @param pid         传感器pid
     * @param enable      enable/disable
     */
    public static CommonData setPilotLamp(int meshAddress, int pid, boolean enable) {
        int pidLow = pid & 0xff;
        int pidHigh = (pid >> 8) & 0xff;
        byte[] params = new byte[]{0x11, (byte) pidLow, (byte) pidHigh, 1, (byte) (enable ? 1 : 0)};
        return packetData(meshAddress, params);
    }

    /**
     * 传感器通用
     * 设置传感器的联动
     *
     * @param meshAddress    单播
     * @param pid            传感器pid
     * @param targetAddress  选择触发场景的范围,(单播,组播,广播)
     * @param triggerSceneId 传感功能生效时触发的场景
     * @param restoreSceneId 恢复常态时触发的场景
     */
    public static CommonData linkScenes(int meshAddress, int pid, int targetAddress, int triggerSceneId, int restoreSceneId) {
        int pidLow = pid & 0xff;
        int pidHigh = (pid >> 8) & 0xff;
        int targetLow = targetAddress & 0xff;
        int targetHigh = (targetAddress >> 8) & 0xff;
        byte[] params = new byte[]{0x11, (byte) pidLow, (byte) pidHigh, 2, (byte) targetLow, (byte) targetHigh, (byte) triggerSceneId, (byte) restoreSceneId};
        return packetData(meshAddress, params);
    }

    /**
     * 传感器通用
     * 指定特定pid进行联动
     *
     * @param meshAddress   单播
     * @param pid           传感器pid
     * @param targetAddress 选择触发场景的范围,(单播,组播,广播)
     * @param linkPid       指定的pid
     */
    public static CommonData linkPid(int meshAddress, int pid, int targetAddress, int linkPid) {
        int pidLow = pid & 0xff;
        int pidHigh = (pid >> 8) & 0xff;
        int targetLow = targetAddress & 0xff;
        int targetHigh = (targetAddress >> 8) & 0xff;
        int linkPidLow = linkPid & 0xff;
        int linkPidHigh = (linkPid >> 8) & 0xff;
        byte[] params = new byte[]{0x11, (byte) pidLow, (byte) pidHigh, 2, (byte) targetLow, (byte) targetHigh, (byte) linkPidLow, (byte) linkPidHigh};
        return packetData(meshAddress, params);
    }


    /**
     * 传感器通用
     * 设置数据上报的时间间隔
     *
     * @param meshAddress 单播
     * @param pid         传感器pid
     * @param second      时间间隔 [5,255]
     */
    public static CommonData setDataReportingInterval(int meshAddress, int pid, int second) {
        int pidLow = pid & 0xff;
        int pidHigh = (pid >> 8) & 0xff;
        if (second < 10 || second > 65535) {
            second = 10;
        }
        byte[] params = new byte[]{0x11, (byte) pidLow, (byte) pidHigh, 3, (byte) second};
        return packetData(meshAddress, params);
    }

    /**
     * 传感器通用
     * 设置触发场景持续的时间,即second秒之后恢复常态,触发未触发时的场景
     *
     * @param meshAddress 单播
     * @param pid         传感器pid
     * @param second      时间间隔 [10,65535]
     */
    public static CommonData setTriggerContinuousTime(int meshAddress, int pid, int second) {
        int pidLow = pid & 0xff;
        int pidHigh = (pid >> 8) & 0xff;
        if (second < 10 || second > 65535) {
            second = 10;
        }
        int delayLow = second & 0xff;
        int delayHigh = (second >> 8) & 0xff;
        byte[] params = new byte[]{0x11, (byte) pidLow, (byte) pidHigh, (byte) 4, (byte) delayLow, (byte) delayHigh};
        return packetData(meshAddress, params);
    }

    public static final int FUNC_ON_OFF = 0;
    public static final int FUNC_DIM = 1;
    public static final int FUNC_SCENE = 2;

    /**
     * 设置光感传感器和红外光感传感器的功能
     *
     * @param meshAddress 单播
     * @param pid         传感器pid
     * @param func        {@link #FUNC_ON_OFF}
     *                    {@link #FUNC_DIM}
     *                    {@link #FUNC_SCENE}
     * @return
     */
    public static CommonData setSensorFunc(int meshAddress, int pid, int func) {
        int pidLow = pid & 0xff;
        int pidHigh = (pid >> 8) & 0xff;
        byte[] params = new byte[]{0x11, (byte) pidLow, (byte) pidHigh, 5, (byte) func};
        return packetData(meshAddress, params);
    }

    /**
     * 设置光感传感器和红外光感传感器
     *
     * @param meshAddress 单播
     * @param pid         传感器pid
     * @param onLux       ->.开关/场景功能时,
     *                    触发开灯的光照强度
     *                    ->调节功能时，
     *                    为想要调节的光照强度
     */
    public static CommonData setLightSensorLuxThreshold(int meshAddress, int pid, int onLux, boolean plusOrMinus) {
        int pidLow = pid & 0xff;
        int pidHigh = (pid >> 8) & 0xff;
        int onLuxLow = onLux & 0xff;
        int onLuxHigh = (onLux >> 8) & 0xff;
        byte[] params = new byte[]{0x11, (byte) pidLow, (byte) pidHigh, 6, (byte) onLuxLow, (byte) onLuxHigh, (byte) 0, (byte) 0, (byte) (plusOrMinus ? 1 : 0)};
        return packetData(meshAddress, params);
    }


    /**
     * 设置光照误差值
     *
     * @param meshAddress 单播
     * @param pid         传感器pid
     * @param lux         光照允许误差(只有调节功能时才需要)
     */
    public static CommonData setLightSensorLuxMistakeValue(int meshAddress, int pid, int lux) {
        int pidLow = pid & 0xff;
        int pidHigh = (pid >> 8) & 0xff;
        int luxLow = lux & 0xff;
        int luxHigh = (lux >> 8) & 0xff;
        byte[] params = new byte[]{0x11, (byte) pidLow, (byte) pidHigh, 7, (byte) luxLow, (byte) luxHigh};
        return packetData(meshAddress, params);
    }

    /**
     * 设置光照误差值
     *
     * @param meshAddress 单播
     * @param pid         传感器pid
     */
    public static CommonData getSensorAllParams(int meshAddress, int pid) {
        int pidLow = pid & 0xff;
        int pidHigh = (pid >> 8) & 0xff;
        byte[] params = new byte[]{0x11, (byte) pidLow, (byte) pidHigh, (byte) 255};
        return packetData(meshAddress, params);
    }

    // TODO: 2017/10/26 红外转蓝牙
    public static CommonData studyIR(int meshAddress) {
        byte[] params = new byte[]{0x10, 1};
        return packetData(meshAddress, params);
    }

    ///////////////////////////////////////////////////////////////////////////
    // 封包
    ///////////////////////////////////////////////////////////////////////////


    /**
     * 未加密前的封包:{@link MeshProtocol#packageCommandData(int, byte[], int)}
     */
    public static CommonData packetData(int meshAddress, byte[] params) {
        byte[] bytes = MeshProtocol.getInstance().packageCommandData(meshAddress, params, Opcode.REQUEST_COMMON);
        return new CommonData(bytes);
    }

    public static CommonData packetData(int meshAddress, byte[] params, byte opCode) {
        byte[] bytes = MeshProtocol.getInstance().packageCommandData(meshAddress, params, opCode);
        return new CommonData(bytes);
    }

    ///////////////////////////////////////////////////////////////////////////
    // OTA
    ///////////////////////////////////////////////////////////////////////////

    public List<byte[]> getFirmwareQueue(byte[] firmware) {
        return pktFirmware(firmware);
    }

    //打包
    public static List<byte[]> pktFirmware(byte[] firmware) {
        List<byte[]> pktList = new ArrayList<>();
        if (firmware == null || firmware.length == 0) {
            Log.e("打包前的验证", "------------------------固件未选取------------------------------------");
            return pktList;
        }

        int index = firmware.length / 16;
        int remain = firmware.length % 16;
        pktList.clear();

        for (int i = 0; i < index; i++) {
            byte[] bytes = new byte[20];
            System.arraycopy(firmware, i * 16, bytes, 2, 16);
            int l = i & 0xff;
            int h = (i >> 8) & 0xff;
            bytes[0] = (byte) l;
            bytes[1] = (byte) h;
            int crc = crc16(bytes);
            l = crc & 0xff;
            h = (crc >> 8) & 0xff;
            bytes[18] = (byte) l;
            bytes[19] = (byte) h;
            pktList.add(bytes);
        }

        if (remain > 0) {
            byte[] bytes = new byte[20];
            for (int i = 0; i < 20; i++) {
                bytes[i] = (byte) 0xff;
            }
            System.arraycopy(firmware, index * 16, bytes, 2, remain);
            int l = index & 0xff;
            int h = (index >> 8) & 0xff;
            bytes[0] = (byte) l;
            bytes[1] = (byte) h;
            int crc = crc16(bytes);
            l = crc & 0xff;
            h = (crc >> 8) & 0xff;
            bytes[18] = (byte) l;
            bytes[19] = (byte) h;
            pktList.add(bytes);

        }

        int last = pktList.size();
        int b1 = last & 0xff;
        int b2 = (last >> 8) & 0xff;
        byte[] temp = new byte[4];
        for (int i = 0; i < temp.length; i++) {
            temp[i] = (byte) 0xff;
        }
        temp[0] = (byte) b1;
        temp[1] = (byte) b2;
        int crc = crc16(temp);
        int b3 = crc & 0xff;
        int b4 = (crc >> 8) & 0xff;
        byte[] lastBytes = {(byte) b1, (byte) b2, (byte) b3, (byte) b4};
        pktList.add(lastBytes);

        Log.e("打包", "------------------------------------打包完成-----------------------------------" + pktList.size());
        return pktList;
    }


    //头尾校验
    private static int crc16(byte[] packet) {

        Log.e("crc16", "packet len" + packet.length);
        int length = packet.length - 2;
        short[] poly = new short[]{0, (short) 0xA001};
        int crc = 0xFFFF;
        int ds;

        for (int j = 0; j < length; j++) {
            ds = packet[j];

            for (int i = 0; i < 8; i++) {
                crc = (crc >> 1) ^ poly[(crc ^ ds) & 1] & 0xFFFF;
                ds = ds >> 1;
            }
        }
        return crc;
    }

    ///////////////////////////////////////////////////////////////////////////
    // other
    ///////////////////////////////////////////////////////////////////////////

    /**
     * 合并两个byte[]
     */
    public static byte[] byteMerger(byte[] byte_1, byte[] byte_2) {
        byte[] byte_3 = new byte[byte_1.length + byte_2.length];
        System.arraycopy(byte_1, 0, byte_3, 0, byte_1.length);
        System.arraycopy(byte_2, 0, byte_3, byte_1.length, byte_2.length);
        return byte_3;
    }

    public static class CommonData {
        byte[] unencryptData;

        CommonData(byte[] unencryptData) {
            this.unencryptData = unencryptData;
        }

        /**
         * <pre class="prettyprint">
         *
         * @throws NullPointerException 如果在加密前没有登录,lib将无法生成加密所需的SessionKey,这个是NDK级的异常
         */
        public byte[] getData(boolean encryptOrNot) {
            Log.e("myGrouptest ", ArraysUtils.bytesToHexString(unencryptData, ","));
            if (encryptOrNot) {
                try {
                    return MeshProtocol.getInstance().encryptData(unencryptData);
                } catch (Exception e) {
                    return unencryptData;
                }
            } else {
                return unencryptData;
            }
        }
    }

    public static final class Opcode {
        public static final byte REQUEST_COMMON = (byte) 0xE4;//控制
        public static final byte REQUEST_SCENE = (byte) 0xC0;//获取设备场景
        public static final byte RESPONSE_SCENE = (byte) 0xC1;//获取场景对应的响应

        public static final byte REQUEST_DEVICE_DETAIL = (byte) 0xDA;//获取设备状态详情
        public static final byte RESPONSE_DEVICE_DETAIL = (byte) 0xDB;

        public static final byte RESPONSE_DEVICE_LIST = (byte) 0xDC;//获取设备对应的响应meshAddress,seq,on/off,pid(2byte)

        public static final byte RESPONSE_SENSOR_DATA_REPORT = (byte) 0xEA;

    }


}
