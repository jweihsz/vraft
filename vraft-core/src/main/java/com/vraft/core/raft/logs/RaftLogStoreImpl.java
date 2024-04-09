package com.vraft.core.raft.logs;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import com.vraft.core.utils.RequireUtil;
import com.vraft.facade.raft.logs.RaftLogOpts;
import com.vraft.facade.raft.logs.RaftLogStore;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.rocksdb.BlockBasedTableConfig;
import org.rocksdb.BloomFilter;
import org.rocksdb.ColumnFamilyDescriptor;
import org.rocksdb.ColumnFamilyHandle;
import org.rocksdb.ColumnFamilyOptions;
import org.rocksdb.CompactionStyle;
import org.rocksdb.CompressionType;
import org.rocksdb.DBOptions;
import org.rocksdb.DataBlockIndexType;
import org.rocksdb.FlushOptions;
import org.rocksdb.IndexType;
import org.rocksdb.InfoLogLevel;
import org.rocksdb.LRUCache;
import org.rocksdb.Priority;
import org.rocksdb.RateLimiter;
import org.rocksdb.ReadOptions;
import org.rocksdb.RocksDB;
import org.rocksdb.RocksDBException;
import org.rocksdb.SkipListMemTableConfig;
import org.rocksdb.Statistics;
import org.rocksdb.StatsLevel;
import org.rocksdb.StringAppendOperator;
import org.rocksdb.WALRecoveryMode;
import org.rocksdb.WriteBatch;
import org.rocksdb.WriteOptions;
import org.rocksdb.util.SizeUnit;

import static org.rocksdb.RocksDB.NOT_FOUND;

/**
 * @author jweihsz
 * @version 2024/4/5 07:47
 * copy from rocketmq5.0
 **/
public class RaftLogStoreImpl implements RaftLogStore {
    private final static Logger logger = LogManager.getLogger(RaftLogStoreImpl.class);

    private RocksDB db;
    private String dbPath;
    private boolean readOnly;

    private ReadOptions readOptions;
    private WriteOptions writeOptions;
    private DBOptions dbOpts;
    private RaftLogOpts logOpts;
    private volatile boolean loaded;
    private volatile boolean closed;
    private ColumnFamilyHandle defaultCFHandle;
    private final List<ColumnFamilyOptions> cfOptions;

    static {
        RocksDB.loadLibrary();
    }

    public RaftLogStoreImpl(RaftLogOpts logOpts) {
        this.logOpts = logOpts;
        this.cfOptions = new ArrayList<>();
    }

    @Override
    public void init() throws Exception {
        final String path = logOpts.getPath();
        dbOpts = createConfigDBOptions(path);

        writeOptions = new WriteOptions();
        writeOptions.setSync(false);
        writeOptions.setDisableWAL(true);
        writeOptions.setNoSlowdown(true);

        readOptions = new ReadOptions();
        readOptions.setPrefixSameAsStart(true);
        readOptions.setTotalOrderSeek(false);
        readOptions.setTailing(false);
    }

    // public long getTerm(long groupId, long index) {

    // }

    private void put(ColumnFamilyHandle cfHandle, WriteOptions writeOptions,
        byte[] keyBytes, int keyLen, byte[] valueBytes, int valueLen)
        throws RocksDBException {
        this.db.put(cfHandle, writeOptions, keyBytes,
            0, keyLen, valueBytes, 0, valueLen);
    }

    private void put(ColumnFamilyHandle cfHandle, WriteOptions writeOptions,
        ByteBuffer keyBB, ByteBuffer valueBB) throws RocksDBException {
        this.db.put(cfHandle, writeOptions, keyBB, valueBB);
    }

    private void batchPut(WriteOptions writeOptions,
        WriteBatch batch) throws RocksDBException {
        this.db.write(writeOptions, batch);
    }

    private byte[] get(ColumnFamilyHandle cfHandle,
        ReadOptions readOptions, byte[] keyBytes) throws RocksDBException {
        return this.db.get(cfHandle, readOptions, keyBytes);
    }

    private boolean get(ColumnFamilyHandle cfHandle, ReadOptions readOptions,
        ByteBuffer keyBB, ByteBuffer valueBB) throws RocksDBException {
        return this.db.get(cfHandle, readOptions, keyBB, valueBB) != NOT_FOUND;
    }

    private List<byte[]> multiGet(final ReadOptions readOptions,
        final List<ColumnFamilyHandle> columnFamilyHandleList,
        final List<byte[]> keys) throws RocksDBException {
        return this.db.multiGetAsList(readOptions, columnFamilyHandleList, keys);
    }

    private void delete(ColumnFamilyHandle cfHandle, WriteOptions writeOptions,
        byte[] keyBytes) throws RocksDBException {
        this.db.delete(cfHandle, writeOptions, keyBytes);
    }

    private void delete(ColumnFamilyHandle cfHandle, WriteOptions writeOptions,
        ByteBuffer keyBB) throws RocksDBException {
        this.db.delete(cfHandle, writeOptions, keyBB);
    }

    private void rangeDelete(ColumnFamilyHandle cfHandle, WriteOptions writeOptions,
        final byte[] startKey, final byte[] endKey) throws RocksDBException {
        this.db.deleteRange(cfHandle, writeOptions, startKey, endKey);
    }

    private void open(List<ColumnFamilyDescriptor> cfDescriptors,
        List<ColumnFamilyHandle> cfHandles) throws RocksDBException {
        this.db = open(this.dbPath, readOnly, this.dbOpts, cfHandles, cfDescriptors);
        this.db.getEnv().setBackgroundThreads(8, Priority.HIGH);
        this.db.getEnv().setBackgroundThreads(8, Priority.LOW);
        RequireUtil.nonNull(db);
    }

    private RocksDB open(String dbPath, boolean readOnly,
        DBOptions options, List<ColumnFamilyHandle> cfHandles,
        List<ColumnFamilyDescriptor> cfDescriptors) throws RocksDBException {
        if (!readOnly) {
            return RocksDB.open(options, dbPath, cfDescriptors, cfHandles);
        } else {
            return RocksDB.openReadOnly(options, dbPath, cfDescriptors, cfHandles);
        }
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
            if (this.readOptions != null) {
                this.readOptions.close();
            }
            if (this.dbOpts != null) {
                this.dbOpts.close();
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
            this.writeOptions = null;
            this.dbOpts = null;
            this.loaded = false;
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void flush(final FlushOptions flushOptions) throws Exception {
        if (!this.loaded || closed) {return;}
        if (this.readOnly || db == null) {return;}
        this.db.flush(flushOptions);
    }

    private Statistics getStatistics() {
        return this.dbOpts.statistics();
    }

    private ColumnFamilyHandle getDefaultCFHandle() {
        return defaultCFHandle;
    }

    private void flushWAL() throws RocksDBException {
        this.db.flushWal(true);
    }

    private ColumnFamilyOptions createConfigOptions() {
        BlockBasedTableConfig cfg = new BlockBasedTableConfig()
            .setFormatVersion(5)
            .setIndexType(IndexType.kBinarySearch)
            .setDataBlockIndexType(DataBlockIndexType.kDataBlockBinarySearch)
            .setBlockSize(32 * SizeUnit.KB)
            .setFilterPolicy(new BloomFilter(16, false))
            .setCacheIndexAndFilterBlocks(false)
            .setCacheIndexAndFilterBlocksWithHighPriority(true)
            .setPinL0FilterAndIndexBlocksInCache(false)
            .setPinTopLevelIndexAndFilter(true)
            .setBlockCache(new LRUCache(4 * SizeUnit.MB, 8, false))
            .setWholeKeyFiltering(true);
        ColumnFamilyOptions options = new ColumnFamilyOptions();
        return options.setMaxWriteBufferNumber(2)
            .setWriteBufferSize(8 * SizeUnit.MB)
            .setMinWriteBufferNumberToMerge(1)
            .setTableFormatConfig(cfg)
            .setMemTableConfig(new SkipListMemTableConfig())
            .setCompressionType(CompressionType.NO_COMPRESSION)
            .setNumLevels(7)
            .setCompactionStyle(CompactionStyle.LEVEL)
            .setLevel0FileNumCompactionTrigger(4)
            .setLevel0SlowdownWritesTrigger(8)
            .setLevel0StopWritesTrigger(12)
            .setTargetFileSizeBase(64 * SizeUnit.MB)
            .setTargetFileSizeMultiplier(2)
            .setMaxBytesForLevelBase(256 * SizeUnit.MB)
            .setMaxBytesForLevelMultiplier(2)
            .setMergeOperator(new StringAppendOperator())
            .setInplaceUpdateSupport(true);
    }

    private DBOptions createConfigDBOptions(String path) {
        DBOptions options = new DBOptions();
        Statistics statistics = new Statistics();
        statistics.setStatsLevel(StatsLevel.EXCEPT_DETAILED_TIMERS);
        return options
            .setDbLogDir(path)
            .setInfoLogLevel(InfoLogLevel.INFO_LEVEL)
            .setWalRecoveryMode(WALRecoveryMode.SkipAnyCorruptedRecords)
            .setManualWalFlush(true)
            .setMaxTotalWalSize(500 * SizeUnit.MB)
            .setWalSizeLimitMB(0)
            .setWalTtlSeconds(0)
            .setCreateIfMissing(true)
            .setCreateMissingColumnFamilies(true)
            .setMaxOpenFiles(-1)
            .setMaxLogFileSize(SizeUnit.GB)
            .setKeepLogFileNum(5)
            .setMaxManifestFileSize(SizeUnit.GB)
            .setAllowConcurrentMemtableWrite(false)
            .setStatistics(statistics)
            .setStatsDumpPeriodSec(600)
            .setAtomicFlush(true)
            .setMaxBackgroundJobs(32)
            .setMaxSubcompactions(4)
            .setParanoidChecks(true)
            .setDelayedWriteRate(16 * SizeUnit.MB)
            .setRateLimiter(new RateLimiter(100 * SizeUnit.MB))
            .setUseDirectIoForFlushAndCompaction(true)
            .setUseDirectReads(true);
    }
}
