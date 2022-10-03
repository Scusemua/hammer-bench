package io.hops.experiments.workload.generator;

import io.hops.experiments.controller.Logger;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class ExistingSubtreeFileGenerator implements FilePool {
    public static final Log LOG = LogFactory.getLog(ExistingSubtreeFileGenerator.class);

    /**
     * The base name used for randomly-generated directories.
     */
    public static final String DIR_NAME = "DirLambda";

    /**
     * The base name used for randomly-generated files.
     */
    public static final String FILE_NAME = "FileLambda";

    /**
     * All the files in this pool.
     */
    protected List<String> filesInPool;

    /**
     * The directories in this pool. These are disjoint from the {@code baseDirectories} set of directories.
     */
    protected List<String> directoriesInPool;

    /**
     * The directories of the starting subtree.
     *
     * We do not modify these, but we can create children within these directories.
     */
    protected List<String> baseDirectories;

    /**
     * The root of the initial subtree.
     */
    protected String baseDirectory;

    private final Random random;

    /**
     * Keeps track of the number of directories we've created.
     * Used when creating names for randomly-generated directories.
     */
    private int numDirectoriesCreated;

    /**
     * Keeps track of the number of directories we've created.
     * Used when creating names for randomly-generated files.
     */
    private int numFilesCreated;

    /**
     * Index of the file we're modifying.
     */
    private int lastModifiedIndex = 0;

    /**
     * @param pathToInitialDirectories Path to a file on-disk that contains all the initial directories to add to this file pool.
     * @param baseDirectory The root directory of the initial subtree.
     */
    public ExistingSubtreeFileGenerator(String pathToInitialDirectories, String baseDirectory) {
        this.baseDirectories = PathUtils.getFilePathsFromFile(pathToInitialDirectories);
        this.filesInPool = new ArrayList<>();
        this.directoriesInPool = new ArrayList<>();
        this.baseDirectory = baseDirectory;
        this.random = new Random();
    }

    /**
     * Return a random directory from the initial subtree.
     */
    private String getRandomBaseDirectory() {
        int idx = random.nextInt(baseDirectories.size());
        return baseDirectories.get(idx);
    }

    /**
     * Return a random, existing file.
     */
    private String getRandomFile() {
        if (!filesInPool.isEmpty()) {
            int idx = random.nextInt(filesInPool.size());
            return filesInPool.get(idx);
        }

        LOG.error("Unable to getRandomFile from file pool: " + this + ".");
        Logger.printMsg("Error: Unable to getRandomFile from file pool: " + this + ".");
        return null;
    }

    /**
     * Return a random directory from all directories we've created and the base subtree.
     */
    private String getRandomDirectory() {
        // Select a directory from both the base subtree and any directories we've since created.
        int idx = random.nextInt(baseDirectories.size() + directoriesInPool.size() - 1);

        // If the index is greater than the size of the base subtree, then we're picking a directory we've generated.
        if (idx >= baseDirectories.size())
            return directoriesInPool.get(idx - baseDirectories.size());

        return baseDirectories.get(idx);
    }

    @Override
    public String getDirToCreate() {
        // All of these should contain the trailing '/' symbol.
        return getRandomDirectory() + DIR_NAME + numDirectoriesCreated++;
    }

    @Override
    public String getFileToCreate() {
        return getRandomDirectory() + FILE_NAME + numFilesCreated++;
    }

    @Override
    public void fileCreationSucceeded(String file) {
        filesInPool.add(file);
    }

    @Override
    public String getFileToAppend() {
        return getRandomFile();
    }

    @Override
    public String getFileToRead() {
        return getRandomFile();
    }

    @Override
    public String getFileToStat() {
        return getRandomFile();
    }

    @Override
    public String getDirToStat() {
        return getRandomDirectory();
    }

    @Override
    public String getFileToInfo() {
        return getRandomFile();
    }

    @Override
    public String getDirToInfo() {
        return getRandomDirectory();
    }

    @Override
    public String getFileToSetReplication() {
        return getRandomFile();
    }

    @Override
    public String getFilePathToChangePermissions() {
        return getRandomFile();
    }

    @Override
    public String getDirPathToChangePermissions() {
        return getRandomDirectory();
    }

    @Override
    public String getFileToRename() {
        return getRandomFile();
    }

    @Override
    public void fileRenamed(String previousName, String newName) {
        String currentFile = filesInPool.get(lastModifiedIndex);

        if (currentFile.equals(previousName))
            throw new IllegalStateException("Renamed file with old name '" + previousName +
                    "' not found. New name is " + newName);

        filesInPool.set(lastModifiedIndex, newName);
    }

    @Override
    public String getFileToDelete() {
        if (filesInPool.isEmpty()) {
            return null;
        }

        lastModifiedIndex = filesInPool.size() - 1;
        return filesInPool.remove(lastModifiedIndex);
    }

    @Override
    public String getFileToChown() {
        return getRandomFile();
    }

    @Override
    public String getDirToChown() {
        return getRandomDirectory();
    }

    @Override
    public long getFileData(byte[] buffer) {
        return 0;
    }

    @Override
    public long getNewFileSize() {
        return 0;
    }

    @Override
    public boolean hasMoreFilesToWrite() {
        return true;
    }
}
