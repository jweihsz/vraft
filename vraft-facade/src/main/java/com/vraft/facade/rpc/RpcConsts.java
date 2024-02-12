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

    public static int RPC_RQ_BITS = 2;
    public static int RPC_TYPE_BITS = 6;

    public static int RPC_ONE_WAY = 1;
    public static int RPC_TWO_WAY = 2;
    public static int RPC_RESPONSE = 0;

    public static int RPC_ADMIN_MSG = 1;
    public static int RPC_CLUSTER_MSG = 2;

    public static short RPC_MAGIC = 0x1210;

    public static byte INVALID_VALUE = (byte)(-1);

}





