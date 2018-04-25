package com.chen.model;

public class FileModel {

    private int id;
    private String name;
    private int parentName;
    private String md5;
    private String time;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getParentName() {
        return parentName;
    }

    public void setParentName(int parentName) {
        this.parentName = parentName;
    }

    public String getMd5() {
        return md5;
    }

    public void setMd5(String md5) {
        this.md5 = md5;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }

    @Override
    public String toString() {
        return "FileModel{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", parentName=" + parentName +
                ", md5='" + md5 + '\'' +
                ", time='" + time + '\'' +
                '}';
    }
}
