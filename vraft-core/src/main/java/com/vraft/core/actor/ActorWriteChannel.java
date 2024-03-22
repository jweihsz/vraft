package com.vraft.core.actor;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

import com.vraft.core.actor.ActorSystem.Actor;
import com.vraft.core.actor.ActorSystem.ActorProcessor;
import com.vraft.core.rpc.RpcCmdExt;
import com.vraft.core.rpc.RpcCommon;
import com.vraft.facade.actor.ActorService;
import com.vraft.facade.rpc.RpcCmd;
import com.vraft.facade.rpc.RpcManager;
import com.vraft.facade.system.SystemCtx;
import com.vraft.facade.uid.UidService;
import io.netty.buffer.ByteBuf;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * @author jweihsz
 * @version 2024/2/21 14:23
 **/
public class ActorWriteChannel implements ActorProcessor<RpcCmd> {
    private final static Logger logger = LogManager.getLogger(ActorWriteChannel.class);

    private final int maxNum = 512;
    private final SystemCtx sysCtx;
    private final Map<Long, Long> maps;
    private final Consumer<Object> rspWatch;

    public ActorWriteChannel(SystemCtx sysCtx) {
        this.sysCtx = sysCtx;
        this.rspWatch = doRspWatch(sysCtx);
        this.maps = new ConcurrentHashMap<>();
    }

    @Override
    public void process(long deadline, Actor<RpcCmd> self) {
        RpcCmd obj = null;
        List<RpcCmd> dataList = self.getDataList();
        do {
            obj = self.getQueue().poll();
            if (obj != null) { dataList.add(obj);}
            if (dataList.size() >= maxNum) {
                RpcCommon.invokeBatch(
                    sysCtx, self.getActorId(),
                    rspWatch, dataList);
                reset(dataList);
            }
            if (obj == null) {break;}
        } while (System.currentTimeMillis() < deadline);
        if (dataList.size() > 0) {
            RpcCommon.invokeBatch(
                sysCtx, self.getActorId(),
                rspWatch, dataList);
            reset(dataList);
        }
    }

    @Override
    public long actorId(long userId, ByteBuf uid) {
        Long actorId = maps.get(userId);
        if (actorId != null) {return actorId;}
        UidService id = sysCtx.getUidSvs();
        actorId = maps.put(userId, id.genActorId());
        return actorId == null ? maps.get(userId) : actorId;
    }

    private void reset(List<RpcCmd> dataList) {
        if (dataList == null || dataList.isEmpty()) {return;}
        for (RpcCmd cmd : dataList) {((RpcCmdExt)cmd).recycle();}
        dataList.clear();
    }

    private Consumer<Object> doRspWatch(SystemCtx sysCtx) {
        final Throwable timeout = new Exception("rpc time out");
        return (param) -> {
            RpcManager rpcMgr = sysCtx.getRpcMgr();
            ActorService actor = sysCtx.getActorSvs();
            if (!(param instanceof RpcCmd)) {return;}
            final RpcCmd temp = (RpcCmd)param;
            final long msgId = temp.getMsgId();
            final long userId = temp.getUserId();
            Object obj = rpcMgr.removePendMsg(userId, msgId);
            if (!(obj instanceof RpcCmd)) {return;}
            final RpcCmd cmd = (RpcCmd)param;
            cmd.setEx(timeout);
            if (actor.dispatchAsyncRsp(userId, cmd)) {return;}
            ((RpcCmdExt)cmd).recycle();
        };
    }

}
