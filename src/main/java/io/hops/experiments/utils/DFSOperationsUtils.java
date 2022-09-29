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
package io.hops.experiments.utils;

import io.hops.experiments.benchmarks.common.BenchMarkFileSystemName;
import io.hops.experiments.workload.generator.FileTreeFromDiskGenerator;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.Path;
import io.hops.metrics.OperationPerformed;
import org.apache.hadoop.hdfs.DistributedFileSystem;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.HashMap;

import io.hops.metrics.TransactionEvent;
import io.hops.metrics.TransactionAttempt;
import io.hops.transaction.context.TransactionsStats;
import io.hops.metrics.OperationPerformed;
import io.hops.leader_election.node.SortedActiveNodeList;
import io.hops.leader_election.node.ActiveNode;
import org.apache.hadoop.hdfs.serverless.consistency.ActiveServerlessNameNodeList;
import org.apache.hadoop.hdfs.serverless.consistency.ActiveServerlessNameNode;

import java.io.EOFException;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Random;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
// import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.List;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.permission.FsPermission;
import io.hops.experiments.workload.generator.FilePool;
import io.hops.experiments.workload.generator.FileTreeGenerator;
import io.hops.experiments.workload.generator.FixeDepthFileTreeGenerator;

public class DFSOperationsUtils {
    public static final Log LOG = LogFactory.getLog(DFSOperationsUtils.class);
    private static final boolean SERVER_LESS_MODE=false; //only for testing. If enabled then the clients will not
    // private static Random rand = new Random(System.currentTimeMillis());
                                                        // contact NNs
    private static ThreadLocal<DistributedFileSystem> dfsClients = new ThreadLocal<>();
    private static ThreadLocal<FilePool> filePools = new ThreadLocal<FilePool>();

    /**
     * Used to cache clients for reuse.
     */
    public static BlockingQueue<DistributedFileSystem> hdfsClientPool
            = new ArrayBlockingQueue<>(1024);

    private static AtomicInteger filePoolCount = new AtomicInteger(0);

    /**
     * Fully-qualified path of hdfs-site.xml configuration file.
     */
    public static String hdfsConfigFilePath = "/home/ubuntu/repos/hops/hadoop-dist/target/hadoop-3.2.0.3-SNAPSHOT/etc/hadoop/hdfs-site.xml";
    public static final String NAME_NODE_ENDPOINT = "hdfs://10.150.0.10:9000/";

    /**
     * Return the OperationPerformed instances.
     */
    public static List<OperationPerformed> getOperationsPerformed() {
        FileSystem client = dfsClients.get();
        if (client == null) {
            LOG.warn("FileSystem client is null. Cannot retrieve 'OperationPerformed' instances.");
            return null;
        }

        if (client instanceof DistributedFileSystem) {
            DistributedFileSystem dfs = (DistributedFileSystem)client;
            return dfs.getOperationsPerformed();
        } else {
            LOG.warn("FileSystem client is not an instance of DistributedFileSystem." +
                    " Cannot retrieve 'OperationPerformed' instances.");
            return null;
        }
    }

    public static ConcurrentHashMap<String, List<TransactionEvent>> getTransactionEvents() {
        FileSystem client = dfsClients.get();
        if (client == null) {
            LOG.warn("FileSystem client is null. Cannot get transaction events.");
            return null;
        }

        if (client instanceof DistributedFileSystem) {
            DistributedFileSystem dfs = (DistributedFileSystem)client;
            return dfs.getTransactionEvents();
        } else {
            LOG.warn("FileSystem client is not an instance of DistributedFileSystem." +
                    " Cannot get transaction events.");
        }
        return null;
    }

    public static void printOperationsPerformed() {
        FileSystem client = dfsClients.get();
        if (client == null) {
            LOG.warn("FileSystem client is null. Cannot print operations performed (i.e., debug info).");
            return;
        }

        if (client instanceof DistributedFileSystem) {
            DistributedFileSystem dfs = (DistributedFileSystem)client;
            dfs.printOperationsPerformed();
        } else {
            LOG.warn("FileSystem client is not an instance of DistributedFileSystem." +
                    " Cannot print operations performed (i.e., debug info).");
        }
    }

    /**
     * Create and return an HDFS Configuration object with the hdfs-site.xml file added as a resource.
     *
     * @param path Fully-qualified path to the configuration file.
     */
    public static Configuration getConfiguration(String path) {
        Configuration configuration = new Configuration();
        LOG.info("Config path: '" + path + "'");
        try {
            File configFile = new File(path);
            URL configFileURL = configFile.toURI().toURL();
            LOG.info("Adding resource to file: " + configFileURL);
            configuration.addResource(configFileURL);
            LOG.info("Successfully added resource to file.");
        } catch (MalformedURLException ex) {
            LOG.error("Invalid path specified for Configuration: '" + path + "':", ex);
            LOG.info("Invalid path specified for Configuration: '" + path + "':", ex);
        } catch (Exception ex) {
            LOG.error("Unexpected error while getting Configuration from file '" + path + "':", ex);
            LOG.info("Unexpected error while getting Configuration from file '" + path + "':", ex);
        }
        return configuration;
    }


    /**
     * Create an HDFS client.
     */
    public static DistributedFileSystem initDfsClient(boolean warmingUp) {
        LOG.info("Creating config.");
        Configuration hdfsConfiguration = getConfiguration(hdfsConfigFilePath);
        LOG.info("Created config.");
        DistributedFileSystem hdfs = new DistributedFileSystem();
        LOG.info("Created DistributedFileSystem instance.");

        try {
            hdfs.initialize(new URI(NAME_NODE_ENDPOINT), hdfsConfiguration);
            LOG.info("Initialized DistributedFileSystem instance.");
        } catch (URISyntaxException | IOException ex) {
            LOG.error("");
            LOG.error("");
            LOG.error("ERROR: Encountered exception while initializing DistributedFileSystem object.");
            ex.printStackTrace();
            System.exit(1);
        }

        // For the HDFS instance we're creating, toggle the consistency protocol + benchmark mode
        // based on whether the client has toggled those options within the benchmarking application.
        // hdfs.setConsistencyProtocolEnabled(consistencyEnabled);
        // hdfs.setBenchmarkModeEnabled(Commands.BENCHMARKING_MODE);

        // The primary HDFS instance should use whatever the default log level is for the HDFS instance we create,
        // as HopsFS has a default log level. If we're creating a non-primary HDFS instance, then we just assign it
        // whatever our primary instance has been set to (as it can change dynamically).
        hdfs.setServerlessFunctionLogLevel("INFO");
        hdfs.setConsistencyProtocolEnabled(!warmingUp);

        return hdfs;
    }

    public static DistributedFileSystem getDFSClient(boolean warmingUp) {
        // DistributedFileSystem client = dfsClients.get();
        DistributedFileSystem client;
        LOG.info("Polling for HDFS client");
        client = hdfsClientPool.poll();

        if (client == null) {
            LOG.info("Could not find client. Creating new HDFS client");
            client = initDfsClient(warmingUp);
            dfsClients.set(client);
        }
        else {
            LOG.info("Found HDFS client");
            // Enable consistency protocol when not warming up.
            client.setConsistencyProtocolEnabled(!warmingUp);
        }

        client.setServerlessFunctionLogLevel("INFO");
        client.setBenchmarkModeEnabled(false); // Want to track cache hits/misses.

        LOG.info("Returning HDFS client");
        return client;
    }

    public static void returnHdfsClient(DistributedFileSystem hdfs) {
        hdfs.clearStatistics(true, true, true);
        hdfsClientPool.add(hdfs);
    }

    public static FilePool getFilePool(String baseDir, int dirsPerDir, int filesPerDir, boolean fixedDepthTree,
                                       int treeDepth, String fileSizeDistribution, boolean readFilesFromDisk,
                                       String diskFilesPath) {
        FilePool filePool = filePools.get();
        if (filePool == null) {
            if(fixedDepthTree) {
                filePool = new FixeDepthFileTreeGenerator(baseDir,treeDepth, fileSizeDistribution);
            }
            else if(readFilesFromDisk) {
                filePool = new FileTreeFromDiskGenerator(baseDir,filesPerDir, dirsPerDir,0, diskFilesPath);
            }
            else {
                filePool = new FileTreeGenerator(baseDir,filesPerDir, dirsPerDir,0, fileSizeDistribution);
            }
            
            filePools.set(filePool);
            LOG.debug("New FilePool " +filePool+" created. Total :"+ filePoolCount.incrementAndGet());
        }else{
            LOG.debug("Reusing file pool obj "+filePool);
        }
        return filePool;
    }
    
    public static void createFile(FileSystem dfs, String pathStr, short replication, FilePool filePool) throws IOException {
        if(SERVER_LESS_MODE){
            serverLessModeRandomWait();
            return;
        }

        if (LOG.isDebugEnabled()) LOG.debug("Creating file " + pathStr);
        FSDataOutputStream out = dfs.create(new Path(pathStr), replication);
        long size = filePool.getNewFileSize();
        if(size > 0){
            byte[] buffer = new byte[64*1024];
            long read = -1;
            do {
                read = filePool.getFileData(buffer);
                if(read > 0){
                    out.write(buffer, 0, (int)read);
                }
            }while( read > -1);
        }

        out.close();
    }

    public static void readFile(FileSystem dfs, String pathStr) throws IOException {
        if (SERVER_LESS_MODE) {
            serverLessModeRandomWait();
            return;
        }

        byte[] buf = new byte[1024 * 1024];
        if (LOG.isDebugEnabled()) LOG.debug("Opening file '" + pathStr + "' now.");
        FSDataInputStream in = dfs.open(new Path(pathStr));
        int read;
        try {
            if (LOG.isDebugEnabled()) LOG.debug("Reading contents of file " + pathStr);
            while ((read = in.read(buf)) > -1) {

                read = in.read(buf);
                while (read > -1) {
                    read = in.read(buf);
                }
            }
        } catch (EOFException e) {
        } finally {
            in.close();
        }
    }

    public static boolean renameFile(FileSystem dfs, Path from, Path to) throws IOException {
        if(SERVER_LESS_MODE){
            serverLessModeRandomWait();
            return true;
        }
        if (LOG.isDebugEnabled()) LOG.debug("Renaming file '" + from + "' to '" + to + "'");
        return dfs.rename(from, to);
    }

    public static boolean deleteFile(FileSystem dfs, String pathStr) throws IOException {
        if(SERVER_LESS_MODE){
            serverLessModeRandomWait();
            return true;
        }
        if (LOG.isDebugEnabled()) LOG.debug("Deleting file '" + pathStr + "'");
        return dfs.delete(new Path(pathStr), true);
    }
    
    public static void ls(FileSystem dfs, String pathStr) throws IOException {
        if(SERVER_LESS_MODE){
            serverLessModeRandomWait();
            return;
        }
       dfs.listStatus(new Path(pathStr));
    }
    
    public static void getInfo(FileSystem dfs, String pathStr) throws IOException {
        if(SERVER_LESS_MODE){
            serverLessModeRandomWait();
            return;
        }
       dfs.getFileStatus(new Path(pathStr));
    }
    
    public static void chmodPath(FileSystem dfs, String pathStr) throws IOException {
        if(SERVER_LESS_MODE){
            serverLessModeRandomWait();
            return;
        }
        dfs.setPermission(new Path(pathStr), new FsPermission((short)0777));
    }
    
    public static void mkdirs(FileSystem dfs, String pathStr) throws IOException {
        if(SERVER_LESS_MODE){
            serverLessModeRandomWait();
            return;
        }
        dfs.mkdirs(new Path(pathStr));
    }
    
    public static void chown(FileSystem dfs, String pathStr) throws IOException {
        if(SERVER_LESS_MODE){
            serverLessModeRandomWait();
            return;
        }
        dfs.setOwner(new Path(pathStr), System.getProperty("user.name"), System.getProperty("user.name"));
    }
    
    public static void setReplication(FileSystem dfs, String pathStr) throws IOException {
        if(SERVER_LESS_MODE){
            serverLessModeRandomWait();
            return;
        }
        dfs.setReplication(new Path(pathStr), (short)3);
    }
    
    public static String round(double val) {
       return String.format("%5s", String.format("%,.4f", val));
    }

    public static String format(int spaces, String string) {
        String format = "%1$-" + spaces + "s";
        return String.format(format, string);
    }

    public static boolean isTwoDecimalPlace(double val) {
        if (val == 0 || val == ((int) val)) {
            return true;
        } else {
            String valStr = Double.toString(val);
            int i = valStr.lastIndexOf('.');
            if (i != -1 && (valStr.substring(i + 1).length() == 1 || valStr.substring(i + 1).length() == 2)) {
                return true;
            } else {
                return false;
            }
        }
    }

    public static void appendFile(FileSystem dfs, String pathStr, long size) throws IOException {
        if (SERVER_LESS_MODE) {
            serverLessModeRandomWait();
            return;
        }

        FSDataOutputStream out = dfs.append(new Path(pathStr));
        if (size != 0) {
            for (long bytesWritten = 0; bytesWritten < size; bytesWritten += 1) {
                out.writeByte(1);
            }
        }
        out.close();
    }

    public static int getActiveNameNodesCount(BenchMarkFileSystemName fsName, FileSystem dfs) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        if (SERVER_LESS_MODE) {
            serverLessModeRandomWait();
            return 0;
        }

        //it only works for HopsFS
        if (fsName == BenchMarkFileSystemName.HopsFS) {
            Class<? extends FileSystem> filesystem = dfs.getClass();
            Method method = filesystem.getMethod("getNameNodesCount");
            Object ret = method.invoke(dfs);
            return (Integer) ret;
        } else if (fsName == BenchMarkFileSystemName.HDFS) {
            return 1;
        } else {
            throw new UnsupportedOperationException("Implement get namenode count for other filesystems");
        }
    }

    private static  void serverLessModeRandomWait(){
//        try {
//            Thread.sleep(rand.nextInt(100));
//            Thread.sleep(1);
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//        }
    }
}
