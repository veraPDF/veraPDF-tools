package org.verapdf.tools;

public class Task {
    public final String inputPath;
    public final String inputVersion;
    public final String targetPath;
    public final String targetVersion;
    public final String outlineHeader;

    public final Boolean isSingleFile;

    public Task(String inputPath, String inputVersion, String targetPath, String targetVersion, String outlineHeader, Boolean isSingleFile) {
        this.inputPath = inputPath;
        this.inputVersion = inputVersion;
        this.targetPath = targetPath;
        this.targetVersion = targetVersion;
        this.outlineHeader = outlineHeader;
        this.isSingleFile = isSingleFile;
    }
}
