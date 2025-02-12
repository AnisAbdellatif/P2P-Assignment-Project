/**
 * The {@code SandboxedFileSystem} class provides a way to manage files within a specified root directory,
 * ensuring that all file operations are confined to the sandboxed root folder.
 */

package de.luh.vss.chat.utils;

import java.io.File;
import java.io.IOException;

public class SandboxedFileSystem {
    private final File rootFolder;

    public SandboxedFileSystem(String rootPath) {
        this.rootFolder = new File(rootPath);
        if (!rootFolder.isDirectory()) {
            throw new IllegalArgumentException("Root path must be a directory");
        }
    }

    public File getFile(String relativePath) throws SecurityException {
        // Resolve the file against the root folder
        File file = new File(rootFolder, relativePath);
        try {
            // Ensure the resolved file is within the root folder
            if (!file.getCanonicalPath().startsWith(rootFolder.getCanonicalPath())) {
                throw new SecurityException("Access outside the root folder is not allowed");
            }
        } catch (IOException e) {
            throw new SecurityException("Error resolving file path", e);
        }
        return file;
    }

    public static File getFileInSandbox(String sandboxRoot, String filePath) {
        SandboxedFileSystem sandboxFS = new SandboxedFileSystem(sandboxRoot);
        return sandboxFS.getFile(filePath);
    }
}