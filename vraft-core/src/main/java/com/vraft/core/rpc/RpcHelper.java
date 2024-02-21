package com.vraft.core.rpc;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import com.vraft.core.actor.ActorSystem;
import com.vraft.core.actor.ActorSystem.Actor;
import com.vraft.core.actor.ActorSystem.ActorProcessor;
import com.vraft.core.pool.ObjectsPool;
import com.vraft.facade.common.CallBack;
import com.vraft.facade.rpc.RpcConsts;
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
    private final ActorSystem actorSys;
    private final Consumer<Object> apply;
    private final BatchWrite batchWrite;
    private final TwoWayResp twoWayResp;
 
    public RpcHelper(SystemCtx sysCtx, RpcManager rpcMgr) {
        this.sysCtx = sysCtx;
        this.rpcMgr = rpcMgr;
        this.apply = buildConsumer(rpcMgr);
        this.actorSys = ActorSystem.INSTANCE;
        this.batchWrite = new BatchWrite(sysCtx, rpcMgr);
        this.twoWayResp = new TwoWayResp(sysCtx, rpcMgr);
    }

    public boolean dispatchRpc(Object channel, byte rq,
        long msgId, String uid, byte[] header, byte[] body,
        long timeout, CallBack cb) throws Exception {
        if (!(channel instanceof Channel)) {return false;}
        final Channel ch = (Channel)channel;
        if (!ch.isWritable()) {return false;}
        final long userId = RpcCommon.getUserId(ch);
        long actorId = RpcCommon.getWriteActor(ch);
        if (userId <= 0 || actorId <= 0) {return false;}
        final RpcCmd cmd = buildBaseCmd(userId, rq,
            msgId, uid, body, header, cb, timeout);
        if (dispatchRpcCmd(actorId, cmd)) {return true;}
        cmd.recycle();
        return false;
    }

    private boolean invokeBatch(Channel ch, List<RpcCmd> nodes) {
        CompositeByteBuf cbf = null;
        if (ch == null || !ch.isWritable()) {return false;}
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

    private boolean invokeOne(Channel ch, RpcCmd cmd) {
        if (ch == null || !ch.isWritable()) {return false;}
        final ByteBuf bf = processRpcCmd(cmd);
        if (bf == null) {return false;}
        ch.writeAndFlush(bf);
        return true;
    }

    private RpcCmd buildBaseCmd(long userId, byte rq,
        long id, String uid, byte[] body, byte[] header,
        CallBack cb, long timeout) {
        RpcCmd cmd = ObjectsPool.RPC_CMD_RECYCLER.get();
        cmd.setCallBack(cb);
        cmd.setMsgId(id);
        cmd.setReq(rq);
        cmd.setUserId(userId);
        cmd.setUid(uid);
        cmd.setHeader(header);
        cmd.setBody(body);
        cmd.setTimeout(timeout);
        cmd.setMsgId(id);
        return cmd;
    }

    private boolean dispatchRpcCmd(long actorId, RpcCmd msg) {
        return actorSys.dispatch(actorId, msg, batchWrite);
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

    private ByteBuf buildOneWayPkg(RpcCmd cmd) {
        return buildBasePkg(
            cmd.getReq(),
            cmd.getMsgId(),
            cmd.getUid(),
            cmd.getHeader(),
            cmd.getBody());
    }

    private ByteBuf buildTwoWayPkg(RpcCmd cmd) {
        final long msgId = genRpcMsgId();
        ByteBuf bf = buildBasePkg(
            cmd.getReq(),
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

    private ByteBuf buildBasePkg(byte rq, long id,
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
        ByteBuf mate = Unpooled.buffer(32);
        mate.writeShort(RpcConsts.RPC_MAGIC);
        mate.writeInt(totalLen);
        mate.writeByte(RpcConsts.RPC_VERSION);
        mate.writeByte(rq);
        mate.writeLong(id);
        mate.writeInt(uidBuf.length);
        mate.writeInt(headerBuf.length);
        mate.writeInt(bodyBuf.length);
        return Unpooled.wrappedBuffer(
            mate.array(), uidBuf, headerBuf, bodyBuf);
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
            long actorId = RpcCommon.getReadActor(ch);
            if (ch == null || actorId <= 0) {return;}
            if (!actorSys.dispatch(actorId, cmd, twoWayResp)) {
                cmd.recycle();
            }
        };
    }

    private class BatchWrite implements ActorProcessor<RpcCmd> {
        private int curSize = 0;
        private final SystemCtx sysCtx;
        private final RpcManager rpcMgr;
        private final ArrayList<RpcCmd> nodes;
        private final int maxNum = 1024;
        private final int maxSize = 524288;

        public BatchWrite(SystemCtx sysCtx,
            RpcManager rpcMgr) {
            this.sysCtx = sysCtx;
            this.rpcMgr = rpcMgr;
            this.nodes = new ArrayList<>(maxNum);
        }

        @Override
        public void process(long deadline, Actor<RpcCmd> self) {
            do {
                RpcCmd cmd = self.getQueue().peek();
                if (cmd == null) {
                    tryHandle(true);
                    break;
                } else {
                    nodes.add(cmd);
                    curSize += calcSize(cmd);
                    tryHandle(false);
                }
            } while (System.currentTimeMillis() <= deadline);
        }

        private void tryHandle(boolean exit) {
            if (nodes.size() <= 0) {return;}
            if (nodes.size() < maxNum
                && curSize < maxSize
                && !exit) {
                return;
            }
            long userId = nodes.get(0).getUserId();
            Channel ch = rpcMgr.getChannel(userId);
            invokeBatch(ch, nodes);
            doAfter();
        }

        private int calcSize(RpcCmd cmd) {
            int size = RpcCommon.RPC_MATE_SIZE;
            if (cmd.getHeader() != null) {
                size += cmd.getHeader().length;
            }
            if (cmd.getBody() != null) {
                size += cmd.getBody().length;
            }
            return size;
        }

        private void doAfter() {
            curSize = 0;
            nodes.clear();
        }
    }

    private class TwoWayResp implements ActorProcessor<RpcCmd> {
        private final SystemCtx sysCtx;
        private final RpcManager rpcMgr;

        public TwoWayResp(SystemCtx sysCtx,
            RpcManager rpcMgr) {
            this.sysCtx = sysCtx;
            this.rpcMgr = rpcMgr;
        }

        @Override
        public void process(long deadline, Actor<RpcCmd> self) {
            do {
                RpcCmd cmd = self.getQueue().peek();
                if (cmd == null) {break;}
                runCb(cmd);
                cmd.recycle();
            } while (System.currentTimeMillis() <= deadline);
        }

        private void runCb(RpcCmd cmd) {
            try {
                if (cmd.getCallBack() == null) {return;}
                cmd.getCallBack().run(cmd.getHeader(),
                    cmd.getBody(), cmd.getEx());
            } catch (Exception ex) {}
        }
    }

}
