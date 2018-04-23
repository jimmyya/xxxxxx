package com.chen.client.model;

public class FileModel {
    private String oldFileURL;//源文件的全路径
    private String newFileURL;//新文件的全路径

    private String newFilePath;//新文件的父路径
    private String newFileName;//新文件的文件名
    public String getOldFileURL() {
        return oldFileURL;
    }
    public void setOldFileURL(String oldFileURL) {
        this.oldFileURL = oldFileURL;
    }
    public String getNewFileURL() {
        return newFileURL;
    }
    public void setNewFileURL(String newFileURL) {
        this.newFileURL = newFileURL;
    }
    public String getNewFilePath() {
        return newFilePath;
    }
    public void setNewFilePath(String newFilePath) {
        this.newFilePath = newFilePath;
    }
    public String getNewFileName() {
        return newFileName;
    }
    public void setNewFileName(String newFileName) {
        this.newFileName = newFileName;
    }


}
