package com.vraft.core.raft.logs;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import com.vraft.core.utils.RequireUtil;
import com.vraft.facade.raft.logs.RaftLogsStore;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.rocksdb.ColumnFamilyDescriptor;
import org.rocksdb.ColumnFamilyHandle;
import org.rocksdb.ColumnFamilyOptions;
import org.rocksdb.DBOptions;
import org.rocksdb.FlushOptions;
import org.rocksdb.Priority;
import org.rocksdb.ReadOptions;
import org.rocksdb.RocksDB;
import org.rocksdb.RocksDBException;
import org.rocksdb.Statistics;
import org.rocksdb.WriteBatch;
import org.rocksdb.WriteOptions;

import static org.rocksdb.RocksDB.NOT_FOUND;

/**
 * @author jweihsz
 * @version 2024/4/5 07:47
 * copy from rocketmq5.0
 **/
public class RaftLogsStoreImpl implements RaftLogsStore {
    private final static Logger logger = LogManager.getLogger(RaftLogsStoreImpl.class);

    protected String dbPath;
    protected boolean readOnly;
    protected RocksDB db;
    protected DBOptions options;

    protected WriteOptions writeOptions;
    protected WriteOptions ableWalWriteOptions;

    protected ReadOptions readOptions;
    protected ReadOptions totalOrderReadOptions;

    protected ColumnFamilyHandle defaultCFHandle;
    protected final List<ColumnFamilyOptions> cfOptions = new ArrayList();

    protected volatile boolean loaded;
    private volatile boolean closed;

    static {
        RocksDB.loadLibrary();
    }

    public void put(ColumnFamilyHandle cfHandle, WriteOptions writeOptions,
        byte[] keyBytes, int keyLen, byte[] valueBytes, int valueLen)
        throws RocksDBException {
        this.db.put(cfHandle, writeOptions, keyBytes,
            0, keyLen, valueBytes, 0, valueLen);
    }

    public void put(ColumnFamilyHandle cfHandle, WriteOptions writeOptions,
        ByteBuffer keyBB, ByteBuffer valueBB) throws RocksDBException {
        this.db.put(cfHandle, writeOptions, keyBB, valueBB);
    }

    public void batchPut(WriteOptions writeOptions,
        WriteBatch batch) throws RocksDBException {
        this.db.write(writeOptions, batch);
    }

    public byte[] get(ColumnFamilyHandle cfHandle,
        ReadOptions readOptions, byte[] keyBytes) throws RocksDBException {
        return this.db.get(cfHandle, readOptions, keyBytes);
    }

    protected boolean get(ColumnFamilyHandle cfHandle, ReadOptions readOptions,
        ByteBuffer keyBB, ByteBuffer valueBB) throws RocksDBException {
        return this.db.get(cfHandle, readOptions, keyBB, valueBB) != NOT_FOUND;
    }

    protected List<byte[]> multiGet(final ReadOptions readOptions,
        final List<ColumnFamilyHandle> columnFamilyHandleList,
        final List<byte[]> keys) throws RocksDBException {
        return this.db.multiGetAsList(readOptions, columnFamilyHandleList, keys);
    }

    protected void delete(ColumnFamilyHandle cfHandle, WriteOptions writeOptions,
        byte[] keyBytes) throws RocksDBException {
        this.db.delete(cfHandle, writeOptions, keyBytes);
    }

    protected void delete(ColumnFamilyHandle cfHandle, WriteOptions writeOptions,
        ByteBuffer keyBB) throws RocksDBException {
        this.db.delete(cfHandle, writeOptions, keyBB);
    }

    protected void rangeDelete(ColumnFamilyHandle cfHandle, WriteOptions writeOptions,
        final byte[] startKey, final byte[] endKey) throws RocksDBException {
        this.db.deleteRange(cfHandle, writeOptions, startKey, endKey);
    }

    protected void open(final List<ColumnFamilyDescriptor> cfDescriptors,
        final List<ColumnFamilyHandle> cfHandles) throws RocksDBException {
        if (this.readOnly) {
            this.db = RocksDB.openReadOnly(this.options, this.dbPath, cfDescriptors, cfHandles);
        } else {
            this.db = RocksDB.open(this.options, this.dbPath, cfDescriptors, cfHandles);
        }
        this.db.getEnv().setBackgroundThreads(8, Priority.HIGH);
        this.db.getEnv().setBackgroundThreads(8, Priority.LOW);
        RequireUtil.nonNull(db);
    }

    public synchronized void shutdown() {
        try {
            if (!this.loaded) {return;}
            final FlushOptions flushOptions = new FlushOptions();
            flushOptions.setWaitForFlush(true);
            try {
                flush(flushOptions);
            } finally {
                flushOptions.close();
            }
            this.db.cancelAllBackgroundWork(true);
            this.db.pauseBackgroundWork();
            //The close order is matter.
            //1. close column family handles

            this.defaultCFHandle.close();
            //2. close column family options.
            for (final ColumnFamilyOptions opt : this.cfOptions) {
                opt.close();
            }
            //3. close options
            if (this.writeOptions != null) {
                this.writeOptions.close();
            }
            if (this.ableWalWriteOptions != null) {
                this.ableWalWriteOptions.close();
            }
            if (this.readOptions != null) {
                this.readOptions.close();
            }
            if (this.totalOrderReadOptions != null) {
                this.totalOrderReadOptions.close();
            }
            if (this.options != null) {
                this.options.close();
            }
            //4. close db.
            if (db != null && !this.readOnly) {
                this.db.syncWal();
            }
            if (db != null) {
                this.db.closeE();
            }
            //5. help gc.
            this.cfOptions.clear();
            this.db = null;
            this.readOptions = null;
            this.totalOrderReadOptions = null;
            this.writeOptions = null;
            this.ableWalWriteOptions = null;
            this.options = null;
            this.loaded = false;
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public void flush(final FlushOptions flushOptions) throws Exception {
        if (!this.loaded || this.readOnly || closed || db == null) {return;}
        this.db.flush(flushOptions);
    }

    public Statistics getStatistics() {
        return this.options.statistics();
    }

    public ColumnFamilyHandle getDefaultCFHandle() {
        return defaultCFHandle;
    }

    public void flushWAL() throws RocksDBException {
        this.db.flushWal(true);
    }

}
