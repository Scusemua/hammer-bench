/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.hops.experiments.benchmarks.common.config;

/**
 *
 * @author salman
 */
public class ConfigKeys {

    // START OF SERVERLESS
    /**
     * Configuration name for specifying the endpoint to issue HTTP requests to invoke serverless functions.
     */
    public static final String SERVERLESS_ENDPOINT = "serverless.endpoint";

    /**
     * The default endpoint/URI for invoking a serverless function (i.e., namenode).
     */
    public static final String SERVERLESS_ENDPOINT_DEFAULT = "https://34.86.224.47:444/api/v1/web/whisk.system/default/namenode";
    // public static final String SERVERLESS_ENDPOINT_DEFAULT = "https://openwhisk.serverless-mds-cluster-243065a7719552ad2f4388dc81e46642-0000.us-east.containers.appdomain.cloud:443/api/v1/web/whisk.system/default/namenode";

    /**
     * Configuration property for defining the serverless platform in use.
     */
    public static final String SERVERLESS_PLATFORM = "serverless.platform";

    /**
     * The default serverless platform of Serverless HopsFS.
     */
    public static final String SERVERLESS_PLATFORM_DEFAULT = "openwhisk";

    /**
     * If true, then we'll pass an argument to the NNs indicating that they should print their
     * debug output from the underlying NDB C++ library (libndbclient.so).
     */
    public static final String NDB_DEBUG = "storage.ndb.debug.enabled";
    public static final boolean NDB_DEBUG_DEFAULT = false;

    /**
     * This string is passed to the NDB C++ library (on the NameNodes) if NDB debugging is enabled.
     */
    public static final String NDB_DEBUG_STRING = "storage.ndb.debug.string";
    public static final String NDB_DEBUG_STRING_DEFAULT = "d:t:L:F";

    /**
     * The maximum number of uniquely-deployed serverless functions available for use with this particular
     * Serverless HopsFS cluster.
     */
    public static final String SERVERLESS_MAX_DEPLOYMENTS = "serverless.deployments.max";
    public static final int SERVERLESS_MAX_DEPLOYMENTS_DEFAULT = 4;

    public static final String SERVERLESS_METADATA_CACHE_REDIS_ENDPOINT = "serverless.redis.endpoint";
    public static final String SERVERLESS_METADATA_CACHE_REDIS_ENDPOINT_DEFAULT = "127.0.0.1";

    public static final String SERVERLESS_DEFAULT_LOG_LEVEL = "serverless.default.loglevel";
    public static final String SERVERLESS_DEFAULT_LOG_LEVEL_DEFAULT = "DEBUG";

    public static final String SERVERLESS_METADATA_CACHE_REDIS_PORT = "serverless.redis.port";
    public static final int SERVERLESS_METADATA_CACHE_REDIS_PORT_DEFAULT = 6379;

    public static final String SERVERLESS_USE_UDP = "serverless.udp.enabled";
    public static final boolean SERVERLESS_USE_UDP_DEFAULT = false;

    /**
     * Serverless HopsFS clients expose a TCP server that NameNodes establish connections with.
     * Clients can then use TCP requests to communicate with NameNodes.
     */
    public static final String SERVERLESS_TCP_SERVER_PORT = "serverless.tcp.port";

    public static final int SERVERLESS_TCP_SERVER_PORT_DEFAULT = 6000;

    public static final String SERVERLESS_TCP_REQUESTS_ENABLED = "serverless.tcp.enabled";
    public static final boolean SERVERLESS_TCP_REQUESTS_ENABLED_DEFAULT = true;

    public static final String SERVERLESS_HTTP_RETRY_MAX = "serverless.http.maxretries";
    public static final int SERVERLESS_HTTP_RETRY_MAX_DEFAULT = 3;

    /**
     * Comma-delimited list of hostnames of ZooKeeper servers.
     */
    public static final String SERVERLESS_ZOOKEEPER_HOSTNAMES = "serverless.zookeepers.hosts";
    public static final String SERVERLESS_ZOOKEEPER_HOSTNAMES_DEFAULT = "10.241.64.15:2181,10.150.0.19:2181,10.241.64.14:2181";

    /**
     * Time, in seconds, for an HTTP request to a NameNode to timeout. Timed-out
     * requests will be retried according to the SERVERLESS_HTTP_RETRY_MAX
     * configuration parameter.
     */
    public static final String SERVERLESS_HTTP_TIMEOUT = "serverless.http.timeout";
    public static final int SERVERLESS_HTTP_TIMEOUT_DEFAULT = 20;

    public static final String SERVERLESS_METADATA_CACHE_ENABLED = "serverless.metadatacache.enabled";
    public static final boolean SERVERLESS_METADATA_CACHE_ENABLED_DEFAULT = true;

    /**
     * How often, in seconds, the list of active name nodes should be updated.
     */
    public static final String SERVERLESS_ACTIVE_NODE_REFRESH = "serverless.activenodes.refreshinterval";
    public static final int SERVERLESS_ACTIVE_NODE_REFRESH_DEFAULT = 10;

    /**
     * How long to wait for the worker thread to execute a given task before timing out.
     */
    public static final String SERVERLESS_WORKER_THREAD_TIMEOUT_MILLISECONDS = "serverless.task.timeoutmillis";
    public static final int SERVERLESS_WORKER_THREAD_TIMEOUT_MILLISECONDS_DEFAULT = 30000;

    /**
     * How often the worker thread should iterate over its cache to see if any results should be purged.
     */
    public static final String SERVERLESS_PURGE_INTERVAL_MILLISECONDS = "serverless.task.purgeinterval";
    public static final int SERVERLESS_PURGE_INTERVAL_MILLISECONDS_DEFAULT = 300000; // 300 seconds, or 5 minutes.

    /**
     * How long the worker thread should cache previously-computed results before purging them.
     */
    public static final String SERVERLESS_RESULT_CACHE_INTERVAL_MILLISECONDS =  "serverless.task.cacheinterval";
    public static final int SERVERLESS_RESULT_CACHE_INTERVAL_MILLISECONDS_DEFAULT = 180000; // 180 seconds, or 3 minutes.

    /**
     * How long for a connection attempt to the ZooKeeper ensemble to timeout (in milliseconds).
     *
     * ZooKeeper's default is 15,000 milliseconds (so, 15 seconds).
     */
    public static final String SERVERLESS_ZOOKEEPER_CONNECT_TIMEOUT = "serverless.zookeeper.connectiontimeout";
    public static final int SERVERLESS_ZOOKEEPER_CONNECT_TIMEOUT_DEFAULT = 15000;

    /**
     * How long for ZooKeeper to consider a NameNode to have disconnected (in milliseconds).
     *
     * ZooKeeper's default is 60,000 milliseconds (so, 60 seconds), which is too long for our use-case.
     * Transactions will generally time out before then (based on the current configuration at the time of
     * writing this comment).
     *
     * This should be LESS than the transaction acknowledgement phase timeout so that NNs have enough time
     * to detect ZooKeeper membership changes.
     */
    public static final String SERVERLESS_ZOOKEEPER_SESSION_TIMEOUT = "serverless.zookeeper.sessiontimeout";
    public static final int SERVERLESS_ZOOKEEPER_SESSION_TIMEOUT_DEFAULT = 10000;

    /**
     * How long the Leader NN should wait for ACKs before aborting the transaction. As of right now, the
     * Leader NN needs to abort after a little while, or all reads and writes on data modified by the transaction
     * will be blocked (since the transaction locks the rows in intermediate storage).
     *
     * This is in milliseconds.
     */
    public static final String SERVERLESS_TRANSACTION_ACK_TIMEOUT = "serverless.tx.ack.timeout";
    public static final int SERVERLESS_TRANSACTION_ACK_TIMEOUT_DEFAULT = 20000;

    // END OF SERVERLESS

    public static final String BENCHMARK_FILE_SYSTEM_NAME_KEY = "benchmark.filesystem.name";
    public static final String BENCHMARK_FILE_SYSTEM_NAME_DEFAULT = "HDFS";
  
    public static final int BUFFER_SIZE = 4*1024*1024; 
    
    public static final String RAW_RESPONSE_FILE_EXT = ".responses";
    
    public static String MAX_SLAVE_FAILURE_THREASHOLD_KEY = "max.slave.failure.threshold";   // only for logging
    public static int MAX_SLAVE_FAILURE_THREASHOLD_DEFAULT = 0;
    
    public static String NO_OF_NAMENODES_KEY = "no.of.namenodes";   // only for logging
    public static int NO_OF_NAMENODES_DEFAULT = 1;
    
    public static String NO_OF_NDB_DATANODES_KEY = "no.of.ndb.datanodes";   // only for logging
    public static int NO_OF_NDB_DATANODES_DEFAULT = 0;
    
    public static String BENCHMARK_TYPE_KEY = "benchmark.type";
    public static String BENCHMARK_TYPE_DEFAULT = "RAW";// "Type. RAW | INTERLEAVED | BM ."
    
    public static String GENERATE_PERCENTILES_KEY = "generate.percentiles";
    public static boolean   GENERATE_PERCENTILES_DEFAULT = false;
    
    public static String INTERLEAVED_BM_DURATION_KEY = "interleaved.bm.duration";
    public static long   INTERLEAVED_BM_DURATION_DEFAULT = 60*1000;
    
    public static String RAW_CREATE_PHASE_MAX_FILES_TO_CRAETE_KEY = "raw.create.phase.max.files.to.create";
    public static long RAW_CREATE_PHASE_MAX_FILES_TO_CRAETE_DEFAULT = Long.MAX_VALUE;
    
    public static String RAW_CREATE_FILES_PHASE_DURATION_KEY = "raw.create.files.phase.duration";
    public static long    RAW_CREATE_FILES_PHASE_DURATION_DEFAULT = 0; 
    
    public static String INTLVD_CREATE_FILES_PERCENTAGE_KEY = "interleaved.create.files.percentage";
    public static double    INTLVD_CREATE_FILES_PERCENTAGE_DEFAULT = 0; 
    
    public static String RAW_READ_FILES_PHASE_DURATION_KEY = "raw.read.files.phase.duration"; 
    public static long   RAW_READ_FILES_PHASE_DURATION_DEFAULT = 0;
    
    public static String INTLVD_READ_FILES_PERCENTAGE_KEY = "interleaved.read.files.percentage";
    public static double    INTLVD_READ_FILES_PERCENTAGE_DEFAULT = 0; 
     
    public static String RAW_RENAME_FILES_PHASE_DURATION_KEY = "raw.rename.files.phase.duration"; 
    public static long   RAW_RENAME_FILES_PHASE_DURATION_DEFAULT = 0;
    
    public static String INTLVD_RENAME_FILES_PERCENTAGE_KEY = "interleaved.rename.files.percentage";
    public static double    INTLVD_RENAME_FILES_PERCENTAGE_DEFAULT = 0; 
     
    public static String RAW_LS_FILE_PHASE_DURATION_KEY = "raw.ls.files.phase.duration"; 
    public static long   RAW_LS_FILE_PHASE_DURATION_DEFAULT = 0;
    
    public static String INTLVD_LS_FILE_PERCENTAGE_KEY = "interleaved.ls.files.percentage";
    public static double    INTLVD_LS_FILE_PERCENTAGE_DEFAULT = 0; 
    
    public static String RAW_LS_DIR_PHASE_DURATION_KEY = "raw.ls.dirs.phase.duration"; 
    public static long   RAW_LS_DIR_PHASE_DURATION_DEFAULT = 0;
    
    public static String INTERLEAVED_WORKLOAD_NAME_KEY = "interleaved.workload.name";
    public static String INTERLEAVED_WORKLOAD_NAME_DEFAULT = "default"; 
    
    public static String INTLVD_LS_DIR_PERCENTAGE_KEY = "interleaved.ls.dirs.percentage";
    public static double    INTLVD_LS_DIR_PERCENTAGE_DEFAULT = 0; 
    
    public static String RAW_DElETE_FILES_PHASE_DURATION_KEY = "raw.delete.files.phase.duration"; 
    public static long   RAW_DELETE_FILES_PHASE_DURATION_DEFAULT = 0;
    
    public static String INTLVD_DELETE_FILES_PERCENTAGE_KEY = "interleaved.delete.files.percentage";
    public static double    INTLVD_DELETE_FILES_PERCENTAGE_DEFAULT = 0; 
    
    public static String RAW_CHMOD_FILES_PHASE_DURATION_KEY = "raw.chmod.files.phase.duration"; 
    public static long   RAW_CHMOD_FILES_PHASE_DURATION_DEFAULT = 0;
    
    public static String RAW_CHMOD_DIRS_PHASE_DURATION_KEY = "raw.chmod.dirs.phase.duration"; 
    public static long   RAW_CHMOD_DIRS_PHASE_DURATION_DEFAULT = 0;
    
    public static String INTLVD_CHMOD_FILES_PERCENTAGE_KEY = "interleaved.chmod.files.percentage";
    public static double    INTLVD_CHMOD_FILES_PERCENTAGE_DEFAULT = 0; 
    
    public static String INTLVD_CHMOD_DIRS_PERCENTAGE_KEY = "interleaved.chmod.dirs.percentage";
    public static double    INTLVD_CHMOD_DIRS_PERCENTAGE_DEFAULT = 0; 
    
    public static String RAW_MKDIR_PHASE_DURATION_KEY = "raw.mkdir.phase.duration"; 
    public static long   RAW_MKDIR_PHASE_DURATION_DEFAULT = 0;
    
    public static String INTLVD_MKDIR_PERCENTAGE_KEY = "interleaved.mkdir.percentage";
    public static double    INTLVD_MKDIR_PERCENTAGE_DEFAULT = 0; 

    public static String RAW_SETREPLICATION_PHASE_DURATION_KEY = "raw.file.setReplication.phase.duration"; 
    public static long   RAW_SETREPLICATION_PHASE_DURATION_DEFAULT = 0;
    
    public static String INTLVD_SETREPLICATION_PERCENTAGE_KEY = "interleaved.file.setReplication.percentage";
    public static double    INTLVD_SETREPLICATION_PERCENTAGE_DEFAULT = 0;     
    
    public static String RAW_GET_FILE_INFO_PHASE_DURATION_KEY = "raw.file.getInfo.phase.duration"; 
    public static long   RAW_GET_FILE_INFO_PHASE_DURATION_DEFAULT = 0;
    
    public static String INTLVD_GET_FILE_INFO_PERCENTAGE_KEY = "interleaved.file.getInfo.percentage";
    public static double    INTLVD_GET_FILE_INFO_PERCENTAGE_DEFAULT = 0;     
    
    public static String RAW_GET_DIR_INFO_PHASE_DURATION_KEY = "raw.dir.getInfo.phase.duration"; 
    public static long   RAW_GET_DIR_INFO_PHASE_DURATION_DEFAULT = 0;
    
    public static String INTLVD_GET_DIR_INFO_PERCENTAGE_KEY = "interleaved.dir.getInfo.percentage";
    public static double    INTLVD_GET_DIR_INFO_PERCENTAGE_DEFAULT = 0;     
    
    public static String RAW_FILE_APPEND_PHASE_DURATION_KEY = "raw.file.append.phase.duration"; 
    public static long   RAW_FILE_APPEND_PHASE_DURATION_DEFAULT = 0;
    
    public static String INTLVD_APPEND_FILE_PERCENTAGE_KEY = "interleaved.file.append.percentage";
    public static double INTLVD_APPEND_FILE_PERCENTAGE_DEFAULT = 0;     
    
    public static String RAW_FILE_CHANGE_USER_PHASE_DURATION_KEY = "raw.file.change.user.phase.duration"; 
    public static long   RAW_FILE_CHANGE_USER_PHASE_DURATION_DEFAULT = 0;
    
    public static String INTLVD_FILE_CHANGE_USER_PERCENTAGE_KEY = "interleaved.file.change.user.percentage";
    public static double INTLVD_FILE_CHANGE_USER_PERCENTAGE_DEFAULT = 0;     
    
    public static String RAW_DIR_CHANGE_USER_PHASE_DURATION_KEY = "raw.dir.change.user.phase.duration"; 
    public static long   RAW_DIR_CHANGE_USER_PHASE_DURATION_DEFAULT = 0;
    
    public static String INTLVD_DIR_CHANGE_USER_PERCENTAGE_KEY = "interleaved.dir.change.user.percentage";
    public static double INTLVD_DIR_CHANGE_USER_PERCENTAGE_DEFAULT = 0;     
        
    public static String FS_CEPH_IMPL_KEY = "fs.ceph.impl";
    public static String FS_CEPH_IMPL_DEFAULT = "org.apache.hadoop.fs.ceph.CephFileSystem";
    
    public static String CEPH_AUTH_KEYRING_KEY = "ceph.auth.keyring";
    public static String CEPH_AUTH_KEYRING_DEFAULT = "/etc/ceph/ceph.client.admin.keyring";
    
    public static String CEPH_CONF_FILE_KEY = "ceph.conf.file";
    public static String CEPH_CONF_FILE_DEFAULT = "/etc/ceph/ceph.conf";
    
    public static String CEPH_ROOT_DIR_KEY = "ceph.root.dir";
    public static String CEPH_ROOT_DIR_DEFAULT = "/";
            
    public static String CEPH_MON_ADDRESS_KEY = "ceph.mon.address";
    public static String CEPH_MON_ADDRESS_DEFAULT = "machine:6789";
    
    public static String CEPH_AUTH_ID_KEY = "ceph.auth.id";
    public static String CEPH_AUTH_ID_DEFAULT = "user";
    
    public static String BR_BENCHMARK_DURATION_KEY = "br.benchmark.duration";
    public static int BR_BENCHMARK_DURATION_DEFAULT = 0;

    public static String BR_NUM_BLOCKS_PER_REPORT_KEY = "br.blocks.per.report";
    public static int BR_NUM_BLOCKS_PER_REPORT_DEFAULT = 10;

    public static String BR_NUM_BLOCKS_PER_FILE_KEY = "br.blocks.per.file";
    public static int BR_NUM_BLOCKS_PER_FILE_DEFAULT = 10;

    public static String BR_NUM_FILES_PER_DIR_KEY = "br.files.per.dir";
    public static int BR_NUM_FILES_PER_DIR_DEFAULT = 10;

    public static String BR_MAX_BLOCK_SIZE_KEY = "br.max.block.size";
    public static int BR_MAX_BLOCK_SIZE_DEFAULT = 16;

    public static String BR_READ_STATE_FROM_DISK_KEY = "br.read.state.from.disk";
    public static boolean BR_READ_STATE_FROM_DISK_DEFAULT = false;

    public static String BR_WRITE_STATE_TO_DISK_KEY = "br.write.state.to.disk";
    public static boolean BR_WRITE_STATE_TO_DISK_DEFAULT = false;

    public static String BR_NUM_INVALID_BUCKETS_KEY = "br.num.invalid.buckets";
    public static int BR_NUM_INVALID_BUCKETS_DEFAULT = 0;

    public static String BR_INCLUDE_BLOCKS_KEY = "br.include.blocks";
    public static boolean BR_INCLUDE_BLOCKS_DEFAULT = true;

    public static String BR_WARM_UP_PHASE_THREADS_PER_DN_KEY = "br.warmup.phase.threads.per.dn";
    public static int BR_WARM_UP_PHASE_THREADS_PER_DN_DEFAULT = 5;

    public static String BR_ON_DISK_STATE_PATH_KEY = "br.on.disk.state.path";
    public static String BR_ON_DISK_STATE_PATH_DEFAULT = "/tmp/datanodes-state.txt.gz";

    public static String BR_MAX_TIME_BEFORE_NEXT_REPORT =
        "br.max.time.before.nextreport";
    public static int BR_MAX_TIME_BEFORE_NEXT_REPORT_DEFAULT = 5000;

    public static String BR_MIN_TIME_BEFORE_NEXT_REPORT =
        "br.min.time.before.nextreport";
    public static int BR_MIN_TIME_BEFORE_NEXT_REPORT_DEFAULT = 1000;

    public static String BR_PERSIST_DATABASE = "br.persist.database";
    public static String BR_PERSIST_DATABASE_DEFAULT = "example.com:3306";

    public static String REPLICATION_FACTOR_KEY = "replication.factor";
    public static short  REPLICATION_FACTOR_DEFAULT = 3;

    //format list of tuples
    //[(size,percentage),(size,percentage)]
    //[(1024,10),(2048,90)]
    //all percentages should add to 100
    public static String FILE_SIZE_IN_Bytes_KEY= "file.size";
    public static String FILE_SIZE_IN_Bytes_DEFAULT = "[(0,100)]";
    
    public static String APPEND_FILE_SIZE_IN_Bytes_KEY= "append.size";
    public static long   APPEND_FILE_SIZE_IN_Bytes_DEFAULT = 0;

    public static String READ_FILES_FROM_DISK= "read.files.from.disk";
    public static boolean READ_FILES_FROM_DISK_DEFAULT=false;

    public static String DISK_FILES_PATH="disk.files.path";
    public static String DISK_FILES_PATH_DEFAULT="~";
    
    public static String DIR_PER_DIR_KEY= "dir.per.dir";
    public static int    DIR_PER_DIR_DEFAULT = 2;
    
    public static String FILES_PER_DIR_KEY= "files.per.dir";
    public static int    FILES_PER_DIR_DEFAULT = 16;
    
    public static String  ENABLE_FIXED_DEPTH_TREE_KEY = "enable.fixed.depth.tree";
    public static boolean ENABLE_FIXED_DEPTH_TREE_DEFAULT = false;
    
    public static String  TREE_DEPTH_KEY = "tree.depth";
    public static int     TREE_DEPTH_DEFAULT = 3;

    public static String NUM_SLAVE_THREADS_KEY = "num.slave.threads";
    public static int    NUM_SLAVE_THREADS_DEFAULT = 1;
      
    public static String BASE_DIR_KEY = "base.dir";
    public static String BASE_DIR_DEFAULT = "/test";

    public static String  SKIP_ALL_PROMPT_KEY = "skip.all.prompt";
    public static boolean SKIP_ALL_PROMPT_DEFAULT = false;
    
    public static String  ENABLE_REMOTE_LOGGING_KEY = "enable.remote.logging";
    public static boolean ENABLE_REMOTE_LOGGING_DEFAULT = true;
    
    public static String REMOTE_LOGGING_PORT_KEY = "remote.logging.port";
    public static int    REMOTE_LOGGING_PORT_DEFAULT = 6666;
    
    public static String SLAVE_LISTENING_PORT_KEY = "slave.listening.port";
    public static int    SLAVE_LISTENING_PORT_DEFAULT = 5555;
    
    public static String MASTER_LISTENING_PORT_KEY = "master.listening.port";
    public static int    MASTER_LISTENING_PORT_DEFAULT = 4444;
    
    public static String RESULTS_DIR_KEY = "results.dir";
    public static String RESULTS_DIR_DEFAULT =     ".";
    public static String TEXT_RESULT_FILE_NAME =   "hopsresults.txt";
    public static String BINARY_RESULT_FILE_NAME = "hopsresults.hopsbin";
    
    public static String FILES_TO_CRAETE_IN_WARM_UP_PHASE_KEY = "files.to.create.in.warmup.phase";
    public static int    FILES_TO_CRAETE_IN_WARM_UP_PHASE_DEFAULT = 10;
    
    public static String WARM_UP_PHASE_WAIT_TIME_KEY = "warmup.phase.wait.time";
    public static int    WARM_UP_PHASE_WAIT_TIME_DEFAULT = 1 * 60 * 1000;
    
    public static String LIST_OF_SLAVES_KEY = "list.of.slaves";
    public static String LIST_OF_SLAVES_DEFAULT = "localhost";
    
    public static String FS_DEFAULTFS_KEY = "fs.defaultFS";
    public static String FS_DEFAULTFS_DEFAULT = "";

    public static String DFS_NAMESERVICES = "dfs.nameservices";
    public static String DFS_NAMESERVICES_DEFAULT = "mycluster";

    public static String DFS_NAMENODE_SELECTOR_POLICY_KEY="dfs.namenode.selector-policy";
    public static String DFS_NAMENODE_SELECTOR_POLICY_DEFAULT="RANDOM_STICKY";
    
    public static String DFS_CLIENT_REFRESH_NAMENODE_LIST_KEY="dfs.client.refresh.namenode.list";
    public static long   DFS_CLIENT_REFRESH_NAMENODE_LIST_DEFAULT=60*60*1000;

    public static String DFS_CLIENT_MAX_RETRIES_ON_FAILURE_KEY="dfs.clinet.max.retires.on.failure";
    public static int   DFS_CLIENT_MAX_RETRIES_ON_FAILURE_DEFAULT=5;

    public static String DFS_CLIENT_INITIAL_WAIT_ON_FAILURE_KEY="dfs.client.initial.wait.on.retry";
    public static long   DFS_CLIENT_INITIAL_WAIT_ON_FAILURE_DEFAULT=0;

    public static String DFS_STORE_SMALL_FILES_IN_DB =  "dfs.store.small.files.in.db";
    public static final boolean DFS_STORE_SMALL_FILES_IN_DB_DEFAULT = false;

    public static final String DFS_DB_FILE_MAX_SIZE_KEY = "dfs.db.file.max.size";
    public static final int DFS_DB_FILE_MAX_SIZE_DEFAULT = 32*1024; // 32KB

    public static final String DFS_CLIENT_DELAY_BEFORE_FILE_CLOSE_KEY = "dsf.client.delay.before.file.close";
    public static final int DFS_CLIENT_DELAY_BEFORE_FILE_CLOSE_DEFAULT = 0;

    //failover test
    public static String TEST_FAILOVER= "test.failover";
    public static boolean TEST_FAILOVER_DEFAULT = false;

    public static String RESTART_NAMENODE_AFTER_KEY = "restart.a.namenode.after";
    public static long RESTART_NAMENODE_AFTER_DEFAULT = Long.MAX_VALUE;


    public static String FAIL_OVER_TEST_START_TIME_KEY = "failover.test.start.time";
    public static long FAIL_OVER_TEST_START_TIME_DEFAULT = Long.MAX_VALUE;

    public static String FAIL_OVER_TEST_DURATION_KEY = "failover.test.duration";
    public static long FAIL_OVER_TEST_DURATION_DEFAULT = Long.MAX_VALUE;

    public static String FAILOVER_NAMENODES= "failover.namenodes";
    public static String FAILOVER_NAMENODES_DEFAULT = null;

    public static String HADOOP_SBIN= "hadoop.sbin";
    public static String HADOOP_SBIN_DEFAULT = null;

    public static String HADOOP_USER= "hadoop.user";
    public static String HADOOP_USER_DEFAULT = null;

    public static String NAMENOE_RESTART_COMMANDS= "failover.nn.restart.commands";
    public static String NAMENOE_RESTART_COMMANDS_DEFAULT = null;

    public static String NAMENOE_KILLER_HOST_KEY= "namenode.killer";
    public static String NAMENOE_KILLER_HOST_DEFAULT = null;

    public static final String MASTER_SLAVE_WARMUP_DELAY_KEY= "master.slave.warmup.delay";
    public static final int MASTER_SLAVE_WARMUP_DELAY_KEY_DEFAULT = 0;

    public static final String BR_NUM_BUCKETS_KEY= "br.num.buckets";
    public static final int BR_NUM_BUCKETS_DEFAULT = 1000;

    public static final String BR_IGNORE_LOAD_BALANCER_KEY= "br.ignore.load.balancer";
    public static final boolean BR_IGNORE_LOAD_BALANCER__DEFAULT = true;
}
