package com.vraft.facade.rpc;

/**
 * @author jweihsz
 * @version 2024/2/8 23:32
 **/
public class RpcConsts {
    public static int TCP = 1;
    public static int UDP = 2;
    public static int SERVER = 3;
    public static int CLIENT = 4;

    public static byte RPC_ONE_WAY = 1;
    public static byte RPC_TWO_WAY = 2;
    public static byte RPC_RESPONSE = 3;

    public static short RPC_MAGIC = 0x1210;
    public static byte RPC_VERSION = 0x01;

    public static byte INVALID_VALUE = (byte)(-1);

    public static boolean isOneWay(byte type) {
        return (type & 0x03) == RPC_ONE_WAY;
    }

    public static boolean isTwoWay(byte type) {
        return (type & 0x03) == RPC_TWO_WAY;
    }

    public static boolean isResp(byte type) {
        return (type & 0x03) == RPC_RESPONSE;
    }

}





