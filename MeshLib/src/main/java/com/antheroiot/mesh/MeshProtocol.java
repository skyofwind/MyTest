package com.antheroiot.mesh;

import android.util.Log;

import com.telink.crypto.AES;
import com.telink.util.ArraysUtils;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;

/**
 * copy by leo
 *
 * @author Ruifen9
 */
public class MeshProtocol {
    private final static String LOG_TAG = "MeshProtocol";

    public final static UUID SERVICE_MESH = UUID.fromString("4a425453-4720-4d65-7368-204c45441910");
    public final static UUID CHARA_STATUS = UUID.fromString("4a425453-4720-4d65-7368-204c45441911");
    public final static UUID CHARA_COMMAND = UUID.fromString("4a425453-4720-4d65-7368-204c45441912");
    public final static UUID CHARA_OTA = UUID.fromString("4a425453-4720-4d65-7368-204c45441913");
    public final static UUID CHARA_PAIR = UUID.fromString("4a425453-4720-4d65-7368-204c45441914");

    public final static UUID SERVICE_GENERIC_ACCESS = UUID.fromString("00001800-0000-1000-8000-00805f9b34fb");
    public final static UUID CHARA_DEVICE_NAME = UUID.fromString("00002a00-0000-1000-8000-00805f9b34fb");

    public final static UUID SERVICE_INFO = UUID.fromString("0000180a-0000-1000-8000-00805f9b34fb");
    public final static UUID CHARA_FIRMWARE_VERSION = UUID.fromString("00002a26-0000-1000-8000-00805f9b34fb");
    public final static UUID CHARA_HARDWARE_VERSION = UUID.fromString("00002a27-0000-1000-8000-00805f9b34fb");

    public static MeshProtocol getInstance() {
        return SingletonHolder.INSTANCE;
    }

    private byte[] meshName;
    private byte[] password;
    private byte[] mSessionKey;
    private byte[] macByteArray;
    private byte[] macReverseByteArray;
    private byte[] defaultLTK = new byte[]{(byte) 0xC0, (byte) 0xC1,
            (byte) 0xC2, (byte) 0xC3, (byte) 0xC4, (byte) 0xC5, (byte) 0xC6,
            (byte) 0xC7, (byte) 0xD8, (byte) 0xD9, (byte) 0xDA, (byte) 0xDB,
            (byte) 0xDC, (byte) 0xDD, (byte) 0xDE, (byte) 0xDF};
    private byte[] checkBytes = new byte[8];

    public String mac;

    /**
     * 每次登录之前请使用函数
     */
    public void preLogin(String mac, String name, String pwd) {
        this.mac = mac;
        macByteArray = macToByteArray(mac);
        macReverseByteArray = new byte[macByteArray.length];
        System.arraycopy(macByteArray, 0, macReverseByteArray, 0, macByteArray.length);
        ArraysUtils.reverse(macReverseByteArray, 0, macReverseByteArray.length - 1);

        Random random = new Random();
        random.nextBytes(this.checkBytes);//randm在后面验证登录时作为校验使用
        seq = random.nextInt(255);//解决登录后第一次控制不了情况,Android不明显主要是IOS端

        byte[] networkBytes = name.getBytes(Charset.forName("UTF-8"));
        byte[] pwByte = pwd.getBytes(Charset.forName("UTF-8"));
        meshName = copyBytesFixedSize(networkBytes, MAX_MESH_NAME_LENGTH);
        password = copyBytesFixedSize(pwByte, MAX_MESH_NAME_LENGTH);
    }

    /**
     * @param commandData 数据来自于这个方法{@link #packageCommandData(int, byte[], int)}
     */
    byte[] encryptData(byte[] commandData) {
        if (mSessionKey == null || macByteArray == null) {
            return commandData;
        }
        byte[] sk = this.mSessionKey;
        byte[] seqBytes = new byte[3];
        System.arraycopy(commandData, 0, seqBytes, 0, 3);
        byte[] nonce = this.getSecIVM(macByteArray, seqBytes);
        return AES.encrypt(sk, nonce, commandData);
    }

    /**
     * @param data Bluetooth 的Notification返回的的数据
     * @return 解密的数据
     */
    public byte[] decryptData(byte[] data) {
        if (mSessionKey == null) {
            return data;
        }
        byte[] nonce = getSecIVS(macReverseByteArray);
        System.arraycopy(data, 0, nonce, 3, 5);
        return AES.decrypt(mSessionKey, nonce, data);
    }

    byte[] packageCommandData(int targetMeshAddress, byte[] parameters, int op_code) {
        seq++;
        int n = parameters.length + 10;
        int dstLow = targetMeshAddress & 0xff;
        int dstHigh = (targetMeshAddress >> 8) & 0xff;
        byte[] mBytes = new byte[n];
        mBytes[0] = (byte) (seq & 0xff);
        mBytes[1] = (byte) ((seq >> 8) & 0xff);
        mBytes[2] = (byte) ((seq >> 16) & 0xff);
        mBytes[3] = 0x00; // Src address
        mBytes[4] = 0x00;
        mBytes[5] = (byte) dstLow; // Dest address
        mBytes[6] = (byte) dstHigh;
        mBytes[7] = (byte) op_code;
        mBytes[8] = 0x11;
        mBytes[9] = 0x02;
        // assumes parameters contain the length etc 合并byte[]
        System.arraycopy(parameters, 0, mBytes, 10, parameters.length);
        Log.w(LOG_TAG, " dataPackages: " + ArraysUtils.bytesToHexString(mBytes, " , "));
        return mBytes;
    }

    public boolean checkLoginResult(byte[] data) {
        if (data[0] == BLE_GATT_OP_PAIR_ENC_FAIL) {
            return false;
        }
        byte[] pairRand = new byte[16];
        byte[] rands = new byte[8];
        System.arraycopy(data, 1, pairRand, 0, 16);
        System.arraycopy(data, 1, rands, 0, 8);
        try {
            mSessionKey = getSessionKey(meshName, password, checkBytes, rands, pairRand);
            return mSessionKey != null && (data[0] == BLE_GATT_OP_PAIR_ENC_RSP || data[0] == BLE_GATT_OP_PAIR_ENC_SUCCESS);
        } catch (Exception e) {
            Log.e(LOG_TAG, e.getMessage());
            return false;
        }
    }

    /**
     * 账号密码的数据包，发过去给Module做验证
     */
    public byte[] getLoginPacket() {
        byte[] plaintext = new byte[16];
        for (int i = 0; i < 16; i++) {
            plaintext[i] = (byte) (meshName[i] ^ password[i]);
        }
        byte[] sk = new byte[16];
        System.arraycopy(checkBytes, 0, sk, 0, checkBytes.length);
        byte[] encrypted;
        try {
            encrypted = AES.encrypt(sk, plaintext);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        byte[] commandData = new byte[17];
        commandData[0] = BLE_GATT_OP_PAIR_ENC_REQ;
        System.arraycopy(checkBytes, 0, commandData, 1, checkBytes.length);
        System.arraycopy(encrypted, 8, commandData, 9, 8);
        ArraysUtils.reverse(commandData, 9, 16);
        return commandData;
    }

    /**
     * 修改mesh信息
     *
     * @param meshName meshName
     * @param password password
     */
    public List<byte[]> configureMesh(String meshName, String password) {
        try {
            List<byte[]> meshData = new ArrayList<>();
            byte[] meshNameBytes = copyBytesFixedSize(meshName.getBytes("UTF-8"), MAX_MESH_NAME_LENGTH);
            byte[] passwordBytes = copyBytesFixedSize(password.getBytes("UTF-8"), MAX_MESH_NAME_LENGTH);
            //此处采用默认ltk不做更改
            //longtermKey = generateLtk(meshName, password);
            //meshName，password和LTK三者都一样时才认为是同一个Mesh组网内，
            // 所以LTK应有一种独立于账号密码的机制，避免其他设备"误入"我的组网
            byte[] longtermKey = defaultLTK.clone();
            byte[] nn;
            byte[] pwd;
            byte[] ltk;
            nn = AES.encrypt(mSessionKey, meshNameBytes);
            pwd = AES.encrypt(mSessionKey, passwordBytes);
            ltk = AES.encrypt(mSessionKey, longtermKey);
            ArraysUtils.reverse(nn, 0, nn.length - 1);
            ArraysUtils.reverse(pwd, 0, pwd.length - 1);
            ArraysUtils.reverse(ltk, 0, ltk.length - 1);
            byte[] nnData = new byte[17];
            nnData[0] = BLE_GATT_OP_PAIR_NETWORK_NAME;
            System.arraycopy(nn, 0, nnData, 1, nn.length);

            byte[] pwdData = new byte[17];
            pwdData[0] = BLE_GATT_OP_PAIR_PASS;
            System.arraycopy(pwd, 0, pwdData, 1, pwd.length);

            byte[] ltkData = new byte[17];
            ltkData[0] = BLE_GATT_OP_PAIR_LTK;
            System.arraycopy(ltk, 0, ltkData, 1, ltk.length);

            meshData.add(nnData);
            meshData.add(pwdData);
            meshData.add(ltkData);
            return meshData;
        } catch (Exception e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }
    ///////////////////////////////////////////////////////////////////////////
    // 内部方法
    ///////////////////////////////////////////////////////////////////////////

    private int seq = 1;

    private static final byte BLE_GATT_OP_PAIR_NETWORK_NAME = 4;
    private static final byte BLE_GATT_OP_PAIR_PASS = 5;
    private static final byte BLE_GATT_OP_PAIR_LTK = 6;
    private static final byte BLE_GATT_OP_PAIR_ENC_REQ = 12;
    private static final byte BLE_GATT_OP_PAIR_ENC_RSP = 13;
    private static final byte BLE_GATT_OP_PAIR_ENC_FAIL = 14;
    private static final byte BLE_GATT_OP_PAIR_ENC_SUCCESS = 15;
    private static final int MAX_MESH_NAME_LENGTH = 16;

    private byte[] copyBytesFixedSize(byte[] src, int n) {
        byte[] dest = new byte[n];
        java.util.Arrays.fill(dest, (byte) 0);
        int copyLen = Math.min(n, src.length);
        System.arraycopy(src, 0, dest, 0, copyLen);
        return dest;
    }

    private byte[] getSecIVM(byte[] meshAddress, byte[] seqBytes) {
        byte[] ivm = new byte[8];
        System.arraycopy(meshAddress, 2, ivm, 0, 4);
        ArraysUtils.reverse(ivm, 0, 3);
        ivm[4] = 0x01;
        System.arraycopy(seqBytes, 0, ivm, 5, 3);
        return ivm;
    }

    private byte[] getSecIVS(byte[] macAddress) {
        byte[] ivs = new byte[8];
        ivs[0] = macAddress[0];
        ivs[1] = macAddress[1];
        ivs[2] = macAddress[2];
        return ivs;
    }

    private byte[] getSessionKey(byte[] meshName, byte[] password,
                                 byte[] randm, byte[] rands, byte[] sk) throws Exception {
        byte[] key = new byte[16];
        System.arraycopy(rands, 0, key, 0, rands.length);
        byte[] plaintext = new byte[16];
        for (int i = 0; i < 16; i++) {
            plaintext[i] = (byte) (meshName[i] ^ password[i]);
        }
        byte[] encrypted = AES.encrypt(key, plaintext);
        byte[] result = new byte[16];
        System.arraycopy(rands, 0, result, 0, rands.length);
        System.arraycopy(encrypted, 8, result, 8, 8);
        ArraysUtils.reverse(result, 8, 15);
        if (!ArraysUtils.equals(result, sk))
            return null;
        System.arraycopy(randm, 0, key, 0, randm.length);
        System.arraycopy(rands, 0, key, 8, rands.length);
        byte[] sessionKey = AES.encrypt(plaintext, key);
        ArraysUtils.reverse(sessionKey, 0, sessionKey.length - 1);
        return sessionKey;
    }

    // For converting the address to byte[]
    private byte[] macToByteArray(String mac) {
        String addr = mac.replace(":", "");
        int len = addr.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(addr.charAt(i), 16) << 4) + Character.digit(addr.charAt(i + 1), 16));
        }
        return data;
    }

    private static class SingletonHolder {
        static final MeshProtocol INSTANCE = new MeshProtocol();
    }
}
