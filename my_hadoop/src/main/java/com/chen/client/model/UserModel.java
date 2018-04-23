package com.chen.client.model;

import org.apache.commons.net.ftp.FTPClient;

import java.text.SimpleDateFormat;
import java.util.Calendar;

public class UserModel {
    private  String hostname;//主机
    private  int port;//端口号
    private  String username;//用户名
    private  String password;//密码
    private  boolean isLogin;//登陆标志
    private FTPClient ftpClient;//
    private  String workPath;//当前目录




    public UserModel() {
        super();
        this.isLogin=false;
    }

    public String getSimpleDateFormat(Calendar calendar) {
        SimpleDateFormat simpleDateFormat= new SimpleDateFormat("yy-MM-dd hh:mm");
        return simpleDateFormat.format(calendar.getTime());
    }

    public String getHostname() {
        return hostname;
    }
    public void setHostname(String hostname) {
        this.hostname = hostname;
    }
    public int getPort() {
        return port;
    }
    public void setPort(int port) {
        this.port = port;
    }
    public String getUsername() {
        return username;
    }
    public void setUsername(String username) {
        this.username = username;
    }
    public String getPassword() {
        return password;
    }
    public void setPassword(String password) {
        this.password = password;
    }
    public boolean isLogin() {
        return isLogin;
    }
    public void setLogin(boolean isLogin) {
        this.isLogin = isLogin;
    }
    public FTPClient getFtpClient() {
        return ftpClient;
    }
    public void setFtpClient(FTPClient ftpClient) {
        this.ftpClient = ftpClient;
    }

    public String getWorkPath() {
        return workPath;
    }

    public void setWorkPath(String workPath) {
        this.workPath = workPath;
    }



}
