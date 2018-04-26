package com.chen.client.service;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.security.MessageDigest;
import java.util.Properties;
import java.util.Scanner;

import com.chen.client.FtpClient;
import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPReply;
import org.apache.log4j.Logger;

import com.chen.client.model.FileModel;
import com.chen.client.model.UserModel;
import com.chen.client.utils.ScannerUtil;

/**
 * @description this is the ftpsevice
 * 这是一个ftp服务层，主要作用就是翻译用户的输入然后调用工具类，
 * 只做了一些基本的检验
 * @time 2016/6/6
 * @author CHEN
 *
 */
public class FtpService {

    private Logger logger = Logger.getLogger(FtpService.class);

    /**
     * 用户登陆
     *
     * @return
     */
    public boolean loginUser(UserModel user) {
        InetAddress netAddress=null;
        try {
            netAddress = InetAddress.getLocalHost();
            logger.info("记录日记\n"
                    + "访问者ip:"+netAddress.getHostName());
        } catch (UnknownHostException e1) {
            logger.info("获取不到客户端主机");
            logger.error(e1.getMessage(),e1);

        } //获得ftp客户端信息
        boolean flag = false;
        Scanner scanner = ScannerUtil.getScanner();
        System.out.print("ftp> open ");
        user.setHostname(scanner.next());
        System.out.println("连接到" + user.getHostname());
        System.out.print("端口号<" + user.getHostname() + ">:");
        try {
            user.setPort(scanner.nextInt());
        }catch (Exception e) {
            System.out.println("ftp> 参数错误，连接默认端口21");
            user.setPort(21);
        }
        System.out.print("用户 <" + user.getHostname() + ">:");
        user.setUsername(scanner.next());
        System.out.print("密码<" + user.getHostname() + ">:");
        user.setPassword(scanner.next());

        FTPClient ftpClient = new FTPClient();
        ftpClient.setControlEncoding("UTF-8");// 设置编码
        try {
            // 链接服务器
            ftpClient.connect(user.getHostname(), user.getPort());
            // 登陆服务器
            ftpClient.login(user.getUsername(), user.getPassword());
            // 判断是否成功登陆
            if (FTPReply.isPositiveCompletion(ftpClient.getReplyCode())) {
                flag = true;
                ftpClient.setBufferSize(1024);
                ftpClient.setFileType(FTP.BINARY_FILE_TYPE);
                user.setFtpClient(ftpClient);
                user.setLogin(true);
                user.setWorkPath(File.separator);
                System.out.println("登陆成功");

            } else {
                System.out.println("登陆失败");
                ftpClient.disconnect();
            }

        } catch (SocketException e) {
            logger.error(e.getMessage(),e);
        } catch (IOException e) {
            logger.error(e.getMessage(),e);
        }

        return flag;
    }

    /**
     * 读取当前目录所有的文件
     *
     * @return
     * @throws IOException
     */
    public boolean readAllFile(UserModel user) {
        boolean flag = false;
        System.setProperty("org.apache.commons.net.ftp.systemType.default","WINDOWS");
        FTPClient ftpClient = user.getFtpClient();
        FTPFile[] files;
        try {
            files = ftpClient.listFiles(user.getWorkPath());
            for (FTPFile file : files) {
                if (file.isFile()) {
//                    System.out.println(user.getSimpleDateFormat(file
//                            .getTimestamp()) + "	" + file.getName());
                    System.out.println(file.toString());
                } else if (file.isDirectory()) {
                    System.out.println(file.toString());
//                    System.out.println(user.getSimpleDateFormat(file.getTimestamp()) + "	<dir>  /" + file.getName());
                } else {
                    System.out.println("no file or have been broken");
                }
            }
            if(files.length==0) {
                System.out.println("no file or have been broken");
            }
        } catch (IOException e) {
            logger.error(e.getMessage(), e);
        }


        return flag;
    }

    /**
     * 下载一个文件
     * @param user
     * @return
     */
    public boolean getOneFile(UserModel user) {
        boolean flag = false;
        Scanner scanner = ScannerUtil.getScanner();
        FTPClient ftpClient = user.getFtpClient();
        System.out.print("远程文件    ");
        FileModel file = new FileModel();
        file.setOldFileURL(scanner.next());
        System.out.print("本地文件    ");
        file.setNewFileURL(scanner.next());

        // 遍历文件
        try {
            // 切换到相应的文件夹
//            ftpClient.changeWorkingDirectory(user.getWorkPath());
            // 下载文件
            File localFile = new File(file.getNewFileURL());
            OutputStream os = new FileOutputStream(localFile);
            if(ftpClient.retrieveFile(file.getOldFileURL(), os)) {
                System.out.println("下载成功 "+file.getNewFileURL());
            } else {
                System.out.println("下载失败");
            }
            os.close();

        } catch (IOException e) {
            logger.error(e.getMessage(), e);
        }
        return flag;
    }

    /**
     * 上传一个文件
     * @param user
     * @return
     */
    public boolean sendOneFile(UserModel user) {
        boolean flag = false;
        Scanner scanner = ScannerUtil.getScanner();
        FTPClient ftpClient = user.getFtpClient();
        ftpClient.enterLocalActiveMode();
        FileModel file = new FileModel();
        System.out.print("远程文件    ");
        file.setNewFileURL(scanner.next());
        System.out.print("本地文件    ");
        file.setOldFileURL(scanner.next());

        // 分析远程文件
        String fileUrl = file.getNewFileURL();
        String filePath = null;
        try {
            filePath = fileUrl.substring(0, fileUrl.indexOf(File.separator));// 获得父级路径
            if (File.separator.equals(filePath) || filePath.length() == 0) {
                throw new Exception();
            }
        } catch (Exception e) {
            filePath = user.getWorkPath();// 换成当前工作区
        }
        String fileName = fileUrl.substring(fileUrl.indexOf(File.separator) + 1);// 获得文件名
        File originFile = new File(file.getOldFileURL());
        try {
            String md5Str=getMd5ByFile(originFile);
            fileName=fileName+"#"+md5Str;
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        // 上传文件
        try {
            InputStream is = new FileInputStream(originFile);
            // 准备服务器
            ftpClient.setFileType(FTPClient.BINARY_FILE_TYPE);
//            ftpClient.makeDirectory(filePath);
//            ftpClient.changeWorkingDirectory(filePath);
            if(ftpClient.storeFile(fileName, is)) {
                System.out.println("上传成功");
            } else {
                System.out.println("上传失败");
            }
            is.close();
            flag = true;
        } catch (FileNotFoundException e) {
            logger.error(e.getMessage(), e);
        } catch (IOException e) {
            logger.error(e.getMessage(), e);
        }
        return flag;
    }

    /**
     * 修改工作区间
     *
     * @param user
     */
    public void changDirectory(UserModel user) {
        Scanner scanner = ScannerUtil.getScanner();
        String tempPath =  scanner.next();
        FTPClient ftpClient = user.getFtpClient();
//        try {
//            ftpClient.makeDirectory(tempPath);
//        } catch (IOException e) {
//            logger.error(e.getMessage(), e);
//        }

        try {
            if(ftpClient.changeWorkingDirectory(tempPath)) {
                if(tempPath.startsWith("/")) {
                    user.setWorkPath(tempPath);
                } else {
                    if("/".equals(user.getWorkPath())) {
                        user.setWorkPath("/"+tempPath);
                    } else {
                        user.setWorkPath(user.getWorkPath() + "/" + tempPath);
                    }
                }
                user.setFtpClient(ftpClient);
            } else {
                System.out.println("路径不存在");
            }

        } catch (IOException e) {
            logger.error(e.getMessage(), e);
        }

    }

    /**
     * 自动登陆
     *
     * @param user
     */
    public void loginUserByAuto(UserModel user) {
        boolean flag = false;
        System.out.print("ftp> open ");
        // 默认端口号 21
        user.setHostname("127.0.0.1");
        user.setPort(21);
        user.setUsername("chen");
        user.setPassword("chen");

        FTPClient ftpClient = new FTPClient();
        ftpClient.setControlEncoding("UTF-8");// 设置编码
        try {
            // 链接服务器
            ftpClient.connect(user.getHostname(), user.getPort());
            // 登陆服务器
            ftpClient.login(user.getUsername(), user.getPassword());
            // 判断是否成功登陆
            if (FTPReply.isPositiveCompletion(ftpClient.getReplyCode())) {
                flag = true;
                ftpClient.enterLocalActiveMode();
                user.setFtpClient(ftpClient);
                user.setLogin(true);
                user.setWorkPath("/");
                System.out.println("登陆成功");

            } else {
                System.out.println("登陆失败");
            }
        } catch (SocketException e) {
            // logger.error(e.getMessage(),e);
        } catch (IOException e) {
            // logger.error(e.getMessage(),e);
        }

    }

    /**
     * 删除文件
     *
     * @param user
     */
    public void deleteOneFile(UserModel user) {
        boolean flag = false;
        Scanner scanner = ScannerUtil.getScanner();
        FTPClient ftpClient = user.getFtpClient();
        System.out.print("删除文件    ");
        try {
//            ftpClient.changeWorkingDirectory(user.getWorkPath());
            if(ftpClient.deleteFile(scanner.next())) {
                System.out.println("删除成功");
            } else {
                System.out.println("删除失败");
            }
        } catch (IOException e) {
            logger.error(e.getMessage(), e);
        }

    }

    public void mkDir(UserModel user) {
        FTPClient ftpClient=user.getFtpClient();
        Scanner scanner = ScannerUtil.getScanner();
        try {
            if(ftpClient.makeDirectory(scanner.next())) {
                System.out.println("创建文件夹成功");
            } else {
                System.out.println("创建文件夹失败");
            }

        } catch (IOException e) {
            e.printStackTrace();
        }

    }
    /**
     * 断开链接
     *
     * @param user
     */
    public boolean disConnection(UserModel user) {
        try {
            user.getFtpClient().disconnect();
            // 初始化
            user.setFtpClient(null);
            user.setHostname("");
            user.setPort(0);
            user.setLogin(false);
            user.setUsername("");
            user.setPassword("");
            user.setWorkPath(File.separator);
        } catch (IOException e) {
            logger.error(e.getMessage(), e);
        }
        return true;

    }

    private static String getMd5ByFile(File file) throws FileNotFoundException {
        String value = null;
        FileInputStream in = new FileInputStream(file);
        try {
            MappedByteBuffer byteBuffer = in.getChannel().map(FileChannel.MapMode.READ_ONLY, 0, file.length());
            MessageDigest md5 = MessageDigest.getInstance("MD5");
            md5.update(byteBuffer);
            BigInteger bi = new BigInteger(1, md5.digest());
            value = bi.toString(16);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if(null != in) {
                try {
                    in.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return value;
    }



}
