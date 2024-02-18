package com.vraft.core.rpc;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

import com.vraft.core.pool.ObjectsPool;
import com.vraft.facade.common.CallBack;
import com.vraft.facade.rpc.RpcConsts;
import com.vraft.facade.rpc.RpcProcessor;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.CompositeByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;

/**
 * @author jweihsz
 * @version 2024/2/12 10:00
 **/
public abstract class RpcAbstract {
    private final Map<ByteBuf, RpcProcessor<?>> PROCESSOR = new ConcurrentHashMap<>();
    private final Throwable RPC_TIMEOUT = new Exception("rpc time out");
    private final Consumer<Object> apply = (param) -> {
        if (!(param instanceof RpcCmd)) {return;}
        final RpcCmd temp = (RpcCmd)param;
        Object obj = removePend(temp.getUserId(), temp.getMsgId());
        if (!(obj instanceof RpcCmd)) {return;}
        final RpcCmd cmd = (RpcCmd)param;
        if (cmd.getCallBack() != null) {
            cmd.getCallBack().run(null, null, RPC_TIMEOUT);
        }
        cmd.recycle();
    };

    public void unregisterProcessor(String uid) {
        if (uid == null || uid.isEmpty()) {return;}
        PROCESSOR.remove(RpcCommon.convert(uid));
    }

    public RpcProcessor<?> getProcessor(Object uid) {
        if (uid instanceof ByteBuf) {
            return PROCESSOR.get((ByteBuf)uid);
        } else {
            return null;
        }
    }

    public void registerProcessor(String uid, RpcProcessor<?> processor) {
        if (uid == null || uid.isEmpty() || processor == null) {return;}
        final ByteBuf bf = RpcCommon.convert(uid);
        PROCESSOR.put(bf, processor);
    }

    public abstract long genRpcMsgId();

    public abstract Object removePend(long userId, long msgId);

    public abstract boolean addPend(long userId, long msgId, Object obj);

    public abstract Object startTimeout(Consumer<Object> apply, Object param, long delay);

    public boolean invokeBatch(Object channel, List<RpcCmd> nodes) throws Exception {
        int size = 0;
        ByteBuf temp = null;
        CompositeByteBuf cbf = null;
        if (!(channel instanceof Channel)) {return false;}
        final Channel ch = (Channel)channel;
        if (!ch.isWritable()) {return false;}
        if (nodes == null || nodes.isEmpty()) {return false;}
        cbf = Unpooled.compositeBuffer(nodes.size());
        for (RpcCmd cmd : nodes) {
            if (RpcConsts.isOneWay(cmd.getReq())) {
                temp = buildOneWayPkg(cmd);
            } else if (RpcConsts.isResp(cmd.getReq())) {
                temp = buildOneWayPkg(cmd);
            } else if (RpcConsts.isTwoWay(cmd.getReq())) {
                temp = buildTwoWayPkg(cmd);
            }
            if (temp != null) {
                size += 1;
                cbf.addComponent(true, temp);
            }
        }
        if (size <= 0) {
            cbf.release();
            return false;
        } else {
            ch.writeAndFlush(cbf);
            return true;
        }
    }

    public boolean invokeOneway(Object channel, byte rq, byte ty,
        String uid, byte[] header, byte[] body) throws Exception {
        if (!(channel instanceof Channel)) {return false;}
        final Channel ch = (Channel)channel;
        if (!ch.isWritable()) {return false;}
        ch.writeAndFlush(buildOneWayPkg(rq, ty, uid, header, body));
        return true;
    }

    public boolean invokeOneway(Object channel, RpcCmd cmd) throws Exception {
        if (!(channel instanceof Channel)) {return false;}
        final Channel ch = (Channel)channel;
        if (!ch.isWritable()) {return false;}
        ch.writeAndFlush(buildOneWayPkg(cmd));
        return true;
    }

    public boolean invokeTwoWay(Object channel, byte rq, byte ty,
        String uid, byte[] header, byte[] body, long timeout,
        CallBack cb) throws Exception {
        if (!(channel instanceof Channel)) {return false;}
        final Channel ch = (Channel)channel;
        if (!ch.isWritable()) {return false;}
        long userId = RpcCommon.getUserId(ch);
        if (userId <= 0) {return false;}
        ByteBuf bf = buildTwoWayPkg(userId, rq, ty,
            uid, header, body, timeout, cb);
        if (bf == null) {return false;}
        ch.writeAndFlush(bf);
        return true;
    }

    public boolean invokeTwoWay(Object channel, RpcCmd cmd) throws Exception {
        if (!(channel instanceof Channel)) {return false;}
        final Channel ch = (Channel)channel;
        if (!ch.isWritable()) {return false;}
        long userId = RpcCommon.getUserId(ch);
        if (userId <= 0) {return false;}
        ByteBuf bf = buildTwoWayPkg(cmd);
        if (bf == null) {return false;}
        ch.writeAndFlush(bf);
        return true;
    }

    private byte buildType(byte rq, byte type) {
        return (byte)((type & 0xFC) | (rq & 0x03));
    }

    public RpcCmd buildBaseCmd(long userId, byte rq, byte ty,
        long id, String uid, byte[] body, byte[] header,
        CallBack cb, long timeout) {
        RpcCmd cmd = ObjectsPool.RPC_CMD_RECYCLER.get();
        cmd.setCallBack(cb);
        cmd.setMsgId(id);
        cmd.setReq(rq);
        cmd.setTy(ty);
        cmd.setUserId(userId);
        cmd.setUid(uid);
        cmd.setHeader(header);
        cmd.setBody(body);
        cmd.setTimeout(timeout);
        cmd.setMsgId(id);
        return cmd;
    }

    private ByteBuf buildBasePkg(byte rq, byte ty, long id,
        String uid, byte[] header, byte[] body) {
        int totalLen = RpcCommon.RPC_MATE_SIZE;
        byte[] bodyBuf, uidBuf, headerBuf;
        if (body == null) {
            bodyBuf = RpcCommon.EMPTY_BUFFER;
        } else {
            bodyBuf = body;
        }
        totalLen += bodyBuf.length;
        if (header == null) {
            headerBuf = RpcCommon.EMPTY_BUFFER;
        } else {
            headerBuf = header;
        }
        totalLen += headerBuf.length;
        if (uid == null) {
            uidBuf = RpcCommon.EMPTY_BUFFER;
        } else {
            uidBuf = uid.getBytes();
        }
        totalLen += uidBuf.length;
        final byte flag = buildType(rq, ty);
        ByteBuf mate = Unpooled.buffer(32);
        mate.writeShort(RpcConsts.RPC_MAGIC);
        mate.writeInt(totalLen);
        mate.writeByte(RpcConsts.RPC_VERSION);
        mate.writeByte(flag);
        mate.writeLong(id);
        mate.writeInt(uidBuf.length);
        mate.writeInt(headerBuf.length);
        mate.writeInt(bodyBuf.length);
        return Unpooled.wrappedBuffer(mate.array(), uidBuf, headerBuf, bodyBuf);
    }

    private ByteBuf buildOneWayPkg(byte rq, byte ty,
        String uid, byte[] header, byte[] body) {
        final long msgId = 0L;
        return buildBasePkg(rq, ty, msgId, uid, header, body);
    }

    private ByteBuf buildOneWayPkg(RpcCmd cmd) {
        return buildBasePkg(cmd.getReq(), cmd.getTy(), cmd.getMsgId(),
            cmd.getUid(), cmd.getHeader(), cmd.getBody());
    }

    private ByteBuf buildTwoWayPkg(long userId, byte rq, byte ty,
        String uid, byte[] header, byte[] body, long timeout,
        CallBack cb) {
        final long msgId = genRpcMsgId();
        RpcCmd cmd = buildBaseCmd(userId, rq, ty, msgId, uid,
            null, null, cb, timeout);
        ByteBuf bf = buildBasePkg(rq, ty, msgId, uid, header, body);
        Object task = startTimeout(apply, cmd, timeout);
        if (task == null) {
            bf.release();
            cmd.recycle();
            return null;
        } else {
            cmd.setExt(task);
            addPend(userId, msgId, cmd);
            return bf;
        }
    }

    private ByteBuf buildTwoWayPkg(RpcCmd cmd) {
        final long msgId = genRpcMsgId();
        ByteBuf bf = buildBasePkg(cmd.getReq(), cmd.getTy(),
            msgId, cmd.getUid(), cmd.getHeader(), cmd.getBody());
        Object task = startTimeout(apply, cmd, cmd.getTimeout());
        if (task == null) {
            bf.release();
            cmd.recycle();
            return null;
        } else {
            cmd.setExt(task);
            addPend(cmd.getUserId(), msgId, cmd);
            return bf;
        }
    }

}
