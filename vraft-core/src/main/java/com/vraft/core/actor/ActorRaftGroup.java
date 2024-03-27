package com.vraft.core.actor;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.vraft.core.actor.ActorSystem.Actor;
import com.vraft.core.actor.ActorSystem.ActorProcessor;
import com.vraft.core.rpc.RpcCommon;
import com.vraft.facade.rpc.RpcManager;
import com.vraft.facade.rpc.RpcProcessor;
import com.vraft.facade.system.SystemCtx;
import com.vraft.facade.uid.UidService;
import io.netty.buffer.ByteBuf;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * @author jweihsz
 * @version 2024/3/24 17:00
 **/
public class ActorRaftGroup implements ActorProcessor<ByteBuf> {
    private final static Logger logger = LogManager.getLogger(ActorRaftGroup.class);

    private final SystemCtx sysCtx;
    private final int maxNum = 128;
    private final Map<Long, Map<Long, Long>> maps;

    public ActorRaftGroup(SystemCtx sysCtx) {
        this.sysCtx = sysCtx;
        this.maps = new ConcurrentHashMap<>();
    }

    @Override
    public void process(long deadline, Actor<ByteBuf> self) {
        final long groupId = self.getExtId();
        final long nodeId = self.getSubId();
        List<ByteBuf> dataList = self.getDataList();
        do {
            ByteBuf obj = self.getQueue().poll();
            if (obj != null) { dataList.add(obj);}
            if (dataList.size() >= maxNum) {processGroup(dataList);}
            if (obj == null) {break;}
        } while (System.currentTimeMillis() < deadline);
        if (dataList.size() > 0) {processGroup(dataList);}
    }

    @Override
    public long actorId(long extId, long subId) {
        final UidService id = sysCtx.getUidSvs();
        maps.computeIfAbsent(extId, k -> new ConcurrentHashMap<>());
        return maps.get(extId).computeIfAbsent(subId, k -> id.genActorId());
    }

    private void processGroup(List<ByteBuf> dataList) {
        if (dataList == null || dataList.isEmpty()) {return;}
        try {
            Iterator<ByteBuf> it = dataList.iterator();
            while (it.hasNext()) {
                processByteBuf(it.next(), it.hasNext());
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {
            reset(dataList);
        }
    }

    private void reset(List<ByteBuf> dataList) {
        if (dataList == null || dataList.isEmpty()) {return;}
        for (ByteBuf bf : dataList) {bf.release();}
        dataList.clear();
    }

    private void processByteBuf(ByteBuf bf, boolean hasNext) {
        try {
            final RpcManager rpcMgr = sysCtx.getRpcMgr();
            final ByteBuf uid = RpcCommon.getRpcUid(bf);
            final RpcProcessor p = rpcMgr.getProcessor(uid);
            if (p == null) {return;}
            long messageId = RpcCommon.getRpcSeq(bf);
            long connectId = RpcCommon.getGroupId(bf);
            long nodeId = RpcCommon.getNodeId(bf);
            long groupId = RpcCommon.getGroupId(bf);
            byte[] header = RpcCommon.getHeaderBytes(bf);
            byte[] body = RpcCommon.getBodyBytes(bf);
            p.handle(-1L, groupId, nodeId,
                messageId, header, body, hasNext);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
