package com.chen.client;

import java.util.Scanner;


import com.chen.client.interceptor.LoginInterceptor;
import com.chen.client.utils.ScannerUtil;
import com.chen.client.model.UserModel;
import com.chen.client.service.FtpService;
import net.sf.cglib.proxy.Enhancer;


/**
 * @description this is the ftpclient
 * @time 2016/6/6
 * @author CHEN
 *
 */
public class FtpClient {
    private static UserModel user=new UserModel();
    public static void main(String[] args) {
        Enhancer enhancer=new Enhancer();
        enhancer.setSuperclass(FtpService.class);
        enhancer.setCallback(new LoginInterceptor());
        FtpService ftpService=(FtpService) enhancer.create();

        //输出所有的选项
        System.out.println("使用提示\n"
                + "0.退出 :quit\n"
                + "1.开启服务 : ftp\n"//用户登陆之后显示登陆标志
                + "3.上传文件: send\n"
                + "4.下载文件: get\n"
                + "5.删除文件：delete\n"
                + "6.进入某一级目录: cd\n"
                + "7.创建目录: mkdir\n");
        while(true) {
            Scanner scanner= ScannerUtil.getScanner();
            if(user.isLogin()) {
                System.out.print("ftp  "+user.getUsername()+user.getWorkPath()+" > ");
            } else {
                System.out.print("no  ftp >");
            }
            switch (scanner.next().toLowerCase()) {
                case "quit":ftpService.disConnection(user);break;
                case "ftp":ftpService.loginUser(user);break;//登陆
                case "dir":ftpService.readAllFile(user);break;//显示所有文件
                case "send":ftpService.sendOneFile(user);break;//上传文件
                case "get":ftpService.getOneFile(user);break;//下载文件
                case "delete":ftpService.deleteOneFile(user);break;//删除文件
                case "cd":ftpService.changDirectory(user);break;//改变工作区间
                case "auto":ftpService.loginUserByAuto(user);break;//自动登陆
                case "mkdir":ftpService.mkDir(user);break;
                default:System.out.println("指令有误");break;
            }

        }
    }
}
