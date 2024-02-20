package com.vraft.core.rpc;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

import com.vraft.core.pool.ObjectsPool;
import com.vraft.facade.actor.ActorService;
import com.vraft.facade.actor.ActorType;
import com.vraft.facade.common.CallBack;
import com.vraft.facade.rpc.RpcConsts;
import com.vraft.facade.rpc.RpcProcessor;
import com.vraft.facade.system.SystemCtx;
import com.vraft.facade.timer.TimerService;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.CompositeByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;

/**
 * @author jweihsz
 * @version 2024/2/12 10:00
 **/
public class RpcHelper {

    private final SystemCtx sysCtx;
    private final RpcManager rpcMgr;
    private final Consumer<Object> apply;
    private final Map<ByteBuf, RpcProcessor<?>> PROCESSOR;

    public RpcHelper(SystemCtx sysCtx, RpcManager rpcMgr) {
        this.sysCtx = sysCtx;
        this.rpcMgr = rpcMgr;
        this.apply = buildConsumer(rpcMgr);
        this.PROCESSOR = new ConcurrentHashMap<>();
    }

    public void unregisterProcessor(String uid) {
        if (uid == null || uid.isEmpty()) {return;}
        PROCESSOR.remove(RpcCommon.convert(uid));
    }

    public RpcProcessor<?> getProcessor(Object uid) {
        if (!(uid instanceof ByteBuf)) {return null;}
        return PROCESSOR.get((ByteBuf)uid);
    }

    public void registerProcessor(String uid,
        RpcProcessor<?> processor) {
        if (uid == null || uid.isEmpty()
            || processor == null) {return;}
        final ByteBuf bf = RpcCommon.convert(uid);
        PROCESSOR.put(bf, processor);
    }

    public boolean dispatchRpc(Object channel, byte rq, byte ty,
        String uid, byte[] header, byte[] body, long timeout,
        CallBack cb) throws Exception {
        if (!(channel instanceof Channel)) {return false;}
        final Channel ch = (Channel)channel;
        if (!ch.isWritable()) {return false;}
        final long userId = RpcCommon.getUserId(ch);
        final long actorId = RpcCommon.getUserActor(ch);
        if (userId <= 0 || actorId <= 0) {return false;}
        final RpcCmd cmd = buildBaseCmd(userId, rq, ty,
            genRpcMsgId(), uid, body, header, cb, timeout);
        if (dispatchRpcCmd(actorId, cmd)) {return true;}
        cmd.recycle();
        return false;
    }

    public boolean invokeBatch(Object channel, List<RpcCmd> nodes) {
        CompositeByteBuf cbf = null;
        if (!(channel instanceof Channel)) {return false;}
        final Channel ch = (Channel)channel;
        if (!ch.isWritable()) {return false;}
        if (nodes == null || nodes.isEmpty()) {return false;}
        cbf = Unpooled.compositeBuffer(nodes.size());
        int size = 0;
        for (RpcCmd cmd : nodes) {
            ByteBuf bf = processRpcCmd(cmd);
            if (bf == null) {continue;}
            cbf.addComponent(true, bf);
            size += 1;
        }
        if (size <= 0) {
            cbf.release();
            return false;
        } else {
            ch.writeAndFlush(cbf);
            return true;
        }
    }

    public boolean invokeOne(Object channel, RpcCmd cmd) {
        if (!(channel instanceof Channel)) {return false;}
        final Channel ch = (Channel)channel;
        if (!ch.isWritable()) {return false;}
        final ByteBuf bf = processRpcCmd(cmd);
        if (bf == null) {return false;}
        ch.writeAndFlush(bf);
        return true;
    }

    private ByteBuf processRpcCmd(RpcCmd cmd) {
        if (RpcConsts.isOneWay(cmd.getReq())) {
            return buildOneWayPkg(cmd);
        } else if (RpcConsts.isResp(cmd.getReq())) {
            return buildOneWayPkg(cmd);
        } else if (RpcConsts.isTwoWay(cmd.getReq())) {
            return buildTwoWayPkg(cmd);
        }
        return null;
    }

    private ActorType getActorType(byte rq) {
        if (RpcConsts.isResp(rq)) {
            return ActorType.RPC_RESPONSE;
        } else {
            return ActorType.RPC_REQUEST;
        }
    }

    private byte buildType(byte rq, byte type) {
        return (byte)((type & 0xFC) | (rq & 0x03));
    }

    private RpcCmd buildBaseCmd(long userId, byte rq, byte ty,
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
        return Unpooled.wrappedBuffer(
            mate.array(), uidBuf, headerBuf, bodyBuf);
    }

    private ByteBuf buildOneWayPkg(RpcCmd cmd) {
        return buildBasePkg(
            cmd.getReq(),
            cmd.getTy(),
            cmd.getMsgId(),
            cmd.getUid(),
            cmd.getHeader(),
            cmd.getBody());
    }

    private ByteBuf buildTwoWayPkg(RpcCmd cmd) {
        final long msgId = genRpcMsgId();
        ByteBuf bf = buildBasePkg(
            cmd.getReq(), cmd.getTy(),
            msgId, cmd.getUid(),
            cmd.getHeader(), cmd.getBody());
        Object task = startTimeout(cmd, cmd.getTimeout());
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

    private Consumer<Object> buildConsumer(RpcManager rpcMgr) {
        Throwable timeout = new Exception("rpc time out");
        return (param) -> {
            if (!(param instanceof RpcCmd)) {return;}
            final RpcCmd temp = (RpcCmd)param;
            Object obj = removePend(temp.getUserId(), temp.getMsgId());
            if (!(obj instanceof RpcCmd)) {return;}
            final RpcCmd cmd = (RpcCmd)param;
            cmd.setEx(timeout);
            Channel ch = rpcMgr.getChannel(cmd.getUserId());
            long actorId = RpcCommon.getUserActor(ch);
            if (ch == null || actorId <= 0
                || !dispatchRpcCmd(actorId, cmd)) {
                cmd.recycle();
            }
        };
    }

    private long genRpcMsgId() {
        return sysCtx.getUidService().genMsgId();
    }

    private Object removePend(long userId, long msgId) {
        return rpcMgr.removePendMsg(userId, msgId);
    }

    private boolean addPend(long userId,
        long msgId, Object obj) {
        return rpcMgr.addPendMsg(userId, msgId, obj);
    }

    private Object startTimeout(Object param, long delay) {
        TimerService timerService = sysCtx.getTimerService();
        return timerService.addTimeout(apply, param, delay);
    }

    private boolean dispatchRpcCmd(long actorId, RpcCmd msg) {
        ActorService actorService = sysCtx.getActorService();
        final ActorType type = getActorType(msg.getReq());
        return actorService.dispatch(actorId, type, msg);
    }

}
