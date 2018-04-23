package com.chen.service.model;

import com.chen.service.server.InitService;
import com.chen.service.server.LogUserService;
import com.chen.service.utils.FileSpliter;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

import java.io.*;
import java.math.BigInteger;
import java.net.HttpURLConnection;
import java.net.Socket;
import java.net.URL;
import java.net.UnknownHostException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.security.MessageDigest;
import java.util.*;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * 客户端的请求都有这个类执行
 *
 * @author CHEN
 */
public class Command {

    private Socket socket;
    private BufferedReader reader;
    private BufferedWriter writer;
    private User user = new User();
    private String remoteHost;// 远程主机
    private int remotePort;// 远程端口号

    private static final int NUM = InitService.NUM;//将文件进行分割的数目
    private static final List<String> SERVICES = InitService.SERVICES;//服务器列表

    private static Socket dSocket = null;

    private static String[] strs = new String[10];// 用来存储分解的指令//从中可以获得我们要的字符串

    public Command(Socket socket, BufferedReader reader, BufferedWriter writer) {
        super();
        this.socket = socket;
        this.reader = reader;
        this.writer = writer;
        response("220 Welcome to use.");//初始化成功的时候输出
    }

    /**
     * 服务器响应
     *
     * @param str
     */
    private void response(String str) {
        try {
            writer.write(str);
            writer.newLine();
            writer.flush();
            System.out.println("服务响应：" + str);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 打印信息
     *
     * @param dWriter
     * @param str
     */
    private void printStr(BufferedWriter dWriter, String str) {
        try {
            dWriter.write(str);
            dWriter.newLine();
            dWriter.flush();
            System.out.println("打印信息：" + str);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    //判断命令的类型
    public boolean command(String str) {
        try {
            strs = str.split(" ");
        } catch (Exception e) {

            strs[0] = str;
        }
        System.out.println("用户命令：" + user.getUser() + " > " + str);
        str = strs[0];// 命令字
        str = str.toUpperCase();//转成大写

        try {
            switch (str) {
                case "OPTS": {
                    response("332 User required.");// 用户名
                }
                break;
                case "XMKD": {// 创建新文件
                    commandXMKD();
                }
                case "USER": {
                    user.setUser(strs[1]);// 装上名字
                    response("331 Password required.");
                }
                break;
                case "PASS": {
                    commandPass();
                }
                break;
                case "QUIT": {
                    response("221 thank for use.");
                    user.setWorkDir("");
                }
                break;

                case "PORT": {// port IP 地址和两字节的端口 ID
                    commandPORT();
                }// DIR 命令 //接下来执行List命令
                break;
                case "LIST": {// dir命令
                    commandList();
                }
                break;
                case "CWD": {// CD 命令
                    commandCWD();

                }
                break;
                case "DELE": {// CD 命令
                    commandDELE();

                }
                break;
                case "RETR": {// GET 命令 ：下载文件
                    commandRETR();
                }
                break;
                case "STOR": {// SEND 命令：上传文件
                    commandSTOR();
                }
                break;
                default: {
                    response("500 command param error.");
                }
                break;
            }
        } catch (Exception e) {
            response("500 command param error.");// 错误
        }
        return true;
    }



    /**
     * 创建新的文件
     */
    private void commandXMKD() {
        String mkdirFile = user.getWorkDir() + "/" + strs[1];
        File file = new File(mkdirFile);
        if (!file.exists()) {
            file.mkdir();
        }
    }

    /**
     * 删除文件
     */
    private void commandDELE() {
        String deleteFile= "";
        int count=0;//文件计数器
        // 删除属性内容
        String dir = LogUserService.class.getResource("/").getPath();
        dir += "file_name.properties";
        Properties properties=new Properties();
        try (FileInputStream  fis = new FileInputStream(dir)) {
            properties.load(fis);
            deleteFile=properties.getProperty(strs[1]);
            if(deleteFile==null) {
                response("200 File not exist.");//删除完毕
                return ;
            }
            properties.remove(strs[1]);
            Enumeration en=properties.propertyNames();
            while(en.hasMoreElements()) {
                if(deleteFile.equals(properties.getProperty((String)en.nextElement()))) {
                    count++;
                }
            }
            try (FileOutputStream fos = new FileOutputStream(dir)) {
                properties.store(fos,"");
            }
        } catch (Exception e) {
            System.out.println(e);
        }

        dir = LogUserService.class.getResource("/").getPath();
        dir += "file_member.properties";
        properties=new Properties();
        try (FileInputStream  fis = new FileInputStream(dir)) {
            properties.load(fis);
            properties.remove(strs[1]);
            try (FileOutputStream fos = new FileOutputStream(dir)) {
                properties.store(fos,"");
            }
        } catch (Exception e) {
            System.out.println(e);
        }


        if(count==0) {
            // 删除文件
            delFolder(deleteFile);
        }
        response("200 Command complete.");//删除完毕
    }
    /**
     * 上传文件
     * 对文件进行分隔 然后上传
     */
    private void commandSTOR() throws Exception {
         String oldFileUrl = "";
        String propertyFileName="";
        if (strs[1].contains(user.getOriDir())) {// 万一客户直接就把全路径写了呢
            oldFileUrl = strs[1];
        } else {
            oldFileUrl = user.getWorkDir() + File.separator + strs[1];// 请求文件的全路径
        }
        strs[1]=strs[1].split("#")[0];
        propertyFileName=strs[1];
        // 设置配置
        /**
         * 1. 读取配置文件确定没有这个文件名
         * 2. 读取文件确定md5
         * 3.
         */

        FileInputStream fis=null;
        FileOutputStream fos=null;

        String dir = LogUserService.class.getResource("/").getPath();
        Properties properties=new Properties();
        dir += "file_name.properties";
        fis = new FileInputStream(dir);

        properties.load(fis);
        fos = new FileOutputStream(dir);
        Enumeration en =  properties.propertyNames();
        String md5Origin="";
        if(strs[1].split("#").length>=2) {
            md5Origin=strs[1].split("#")[1];
        }
        while(en.hasMoreElements()) {
            String fileName=(String)en.nextElement();
            String fileKey=(String)properties.getProperty(fileName);
            if(fileKey.equals(md5Origin)) {
                 properties.setProperty(fileName,md5Origin);
                 properties.store(fos,"");
                 //  TODO 结束 直接返回
                response("226 Transfer complete.");//传输完毕
                return ;
            }
            if(fileName.equals(strs[1])) {
                String[] tempFileName=strs[1].split("\\.");
                String houZhui="";
                if(tempFileName.length>=2) {
                    houZhui=tempFileName[1];
                }
                if(tempFileName[0].endsWith(")")) {
                    String temp=tempFileName[0].substring(tempFileName[0].indexOf("("),tempFileName[0].indexOf(")"));
                    try {
                        int tempNum=Integer.parseInt(temp)+1;
                        propertyFileName=tempFileName[0].substring(0,tempFileName[0].indexOf("("))
                                +"("+tempNum+")"+houZhui;
                    } catch(Exception e) {
                        System.out.println(e);
                    }
                } else {
                    propertyFileName = tempFileName[0] + "(1)." + houZhui;
                }
                oldFileUrl = user.getWorkDir() + File.separator + propertyFileName;
            }
        }

        Properties memberProperties=new Properties();
        String memberDir =LogUserService.class.getResource("/").getPath()+ "file_member.properties";
        FileInputStream memberFis = new FileInputStream(memberDir);
        memberProperties.load(memberFis);
        FileOutputStream memberFos = new FileOutputStream(memberDir);
        memberProperties.setProperty(propertyFileName,TimeDealer.timeFormat(new Date()));
        memberProperties.store(memberFos,"");

        BufferedOutputStream bos = null;
        BufferedInputStream bis = null;
        // 上传文件
        try {
            dSocket = new Socket(remoteHost, remotePort);
            bos = new BufferedOutputStream(new FileOutputStream(oldFileUrl));//输出文件的位置
            bis = new BufferedInputStream(dSocket.getInputStream());// 客户端塞过来的流
            byte[] buf = new byte[1024];
            int l = 0;
            response("150 Opening connection for " + propertyFileName);//通知解释器 已经准备好接收数据了
            while ((l = bis.read(buf, 0, 1024)) != -1) {
                bos.write(buf, 0, l);
            }
            response("226 Transfer complete.");//传输完毕
        } catch (Exception e) {
            e.printStackTrace();
            response("550 The system cannot find the path specified.");
        } finally {
            try {
                bis.close();
                bos.close();
                dSocket.close();
                dSocket = null;
            } catch (IOException e) {
                e.printStackTrace();
            }

        }

        //对文件进行分割和上传
        List<String> files = new ArrayList<>();
        File originFile = new File(oldFileUrl);

        boolean flag=true;
        // 记录文件的md5
        String md5Str=getMd5ByFile(originFile);
        Enumeration enTemp = properties.propertyNames();
        while(enTemp.hasMoreElements()) {
            String tempStr=(String)enTemp.nextElement();
            if(md5Str.equals(properties.getProperty(tempStr))) {
                flag=false;
            }
        }
        properties.setProperty(propertyFileName,md5Str);
        properties.store(fos,"");
        fis.close();
        fos.close();

        // 分割文件
        if(flag) {
            long byteSize = originFile.length() / NUM;
            ThreadPoolExecutor executor = new ThreadPoolExecutor(NUM, NUM * 2, 1, TimeUnit.SECONDS, new ArrayBlockingQueue<Runnable>(NUM * 2));
            for (int i = 0; i < NUM; i++) {
//            String fileName = originFile.getName().split("\\.")[0];
                String fileName = md5Str;
                StringBuffer partFileName = new StringBuffer(LogUserService.workDir);
                partFileName.append(File.separator);
                partFileName.append(fileName);
                //检查文件是否存在
                File tempFile = new File(partFileName.toString());
                if (!tempFile.exists()) {
                    tempFile.mkdir();
                }
                partFileName.append(File.separator);
                partFileName.append(fileName);
                partFileName.append("_");
                partFileName.append(i);
                partFileName.append(".part");

                executor.execute(new FileSpliter(partFileName.toString(), originFile, i * byteSize, byteSize));
                files.add(partFileName.toString());
            }

            for (int i = 0; i < NUM; i++) {
                File file = new File(files.get(i));//拿出一个文件
                String url = SERVICES.get(i);//拿出一个服务器地址
                url += "/uploadFile";
                CloseableHttpClient client = HttpClients.createDefault();
                HttpPost post = new HttpPost(url);
                MultipartEntityBuilder entityBuild = MultipartEntityBuilder.create();
                FileBody body = new FileBody(file);
                HttpEntity entity = entityBuild.addPart("file", body).build();
                post.setEntity(entity);
                HttpResponse response = null;
                try {
                    response = client.execute(post);
                } catch (IOException e) {
                    e.printStackTrace();
                }
//                if("false".equals(EntityUtils.toString(response.getEntity(),"utf-8"))){
//
//                }
                //再传一个文件到下一个服务器
                int tempi = (i + 1) % NUM;
                url = SERVICES.get(tempi);
                url += "/uploadFile";
                client = HttpClients.createDefault();
                post = new HttpPost(url);
                entityBuild = MultipartEntityBuilder.create();
                body = new FileBody(file);
                entity = entityBuild.addPart("file", body).build();
                post.setEntity(entity);
                response = null;
                try {
                    response = client.execute(post);
                } catch (IOException e) {
                    e.printStackTrace();
                }


            }
            executor.shutdown();
        }
        originFile.delete();

    }

    /**
     * 下载文件
     * <p>
     * 读取服务器列表然后合并文件
     */
    private boolean commandRETR() {
        BufferedInputStream fin = null;
        PrintStream dout = null;
        String oldFileUrl = user.getWorkDir() + File.separator+ strs[1];// 请求文件的全路径
        File file = new File(strs[1]);
        // 文件名换成
//        String fileName = strs[1].split("\\.")[0];
        String fileName = "";
        String dir = LogUserService.class.getResource("/").getPath();
        dir += "file_name.properties";
        try {
            FileInputStream fis = new FileInputStream(dir);
            Properties properties=new Properties();
            properties.load(fis);
            fileName= properties.getProperty(strs[1]);
        } catch (Exception e) {

        }


     /*   if (!file.exists()) {// 万一用户用的是全路径
            file = new File(oldFileUrl);
            if (!file.exists()) { // 万一用的是缺省呢
                response("550 The system cannot find the file specified.");// 没有该文件
                return false;
            }
        }*/
        // 下载文件
        try {
            response("150 Opening  connection for " + strs[1]);
            dSocket = new Socket(remoteHost, remotePort);
            // fin = new BufferedInputStream(new FileInputStream(oldFileUrl));
            dout = new PrintStream(dSocket.getOutputStream(), true);

            int l = 0;


            for (int i = 0; i < NUM; i++) {
                //先从本机读
                StringBuffer fileUrl = new StringBuffer(LogUserService.workDir);
                fileUrl.append(File.separator);
                fileUrl.append(fileName);
                fileUrl.append(File.separator);
                fileUrl.append(fileName);
                fileUrl.append("_");
                fileUrl.append(i);
                fileUrl.append(".part");
                File originFile = new File(fileUrl.toString());
                //假如文件不存在 从远程服务器读
                if (!originFile.exists()) {
                    StringBuffer buffer = new StringBuffer(SERVICES.get(i));
                    buffer.append("//file//");
                    buffer.append(fileName);
                    buffer.append("_");
                    buffer.append(i);
                    buffer.append(".part");
                    System.out.println("下载部分" + buffer.toString());
                    String urlString = buffer.toString();
                    URL url = new URL(urlString);
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    System.out.println("从远程服务器读" + fileName);
                    fin = new BufferedInputStream(conn.getInputStream());
                } else {
                    System.out.println("从本地服务器读" + fileName);
                    fin = new BufferedInputStream(new FileInputStream(originFile));
                }
                byte[] buf = new byte[1024];
                while ((l = fin.read(buf, 0, 1024)) != -1) {
                    dout.write(buf, 0, l);//写入流
                }
            }
            response("226 Transfer complete.");

        } catch (Exception e) {
            e.printStackTrace();
            response("550 The system cannot find the path specified.");
            return false;
        } finally {
            try {
                fin.close();
                dout.close();
                dSocket.close();
                dSocket = null;
            } catch (IOException e) {
                e.printStackTrace();
            }

        }
        return true;
    }

    /**
     * 用来进入某个文件
     */
    private boolean commandCWD() {
        // 怎么说呢，其实很简单吧，应该就是把用户文件工作区拼上请求字符
        if ("/".equals(strs[1]) || "\\".equals(strs[1])) {
            user.setWorkDir(user.getOriDir());
            response("250 Requested file action okay,the directory is "
                    + user.getWorkDir());
            return true;
        }
        // 判断文件夹存不存在
        File workDir = new File(user.getWorkDir());

        File[] files = workDir.listFiles(new FileFilter() {
            @Override
            public boolean accept(File paramFile) {
                if (paramFile.getName().contains("."))
                    return false;
                return true;
            }
        });// 文件夹的文件夹
        boolean flag = false;
        for (File f : files) {
            if (f.getName().equals(strs[1])) {
                flag = true;
                break;
            }
        }
        if (flag) {
            user.setWorkDir(user.getWorkDir() + File.separator + strs[1]);
            response("250 Requested file action okay,the directory is "
                    + user.getWorkDir());
        } else {
            response("550 The directory does not exists");
        }
        response("250 CWD command successful.");
        return true;
    }

    /**
     * Pass 命令:验证密码 strs[1]:命令字符串的第二个 一般是参数
     */
    private void commandPass() {
        // 检查 用户是否存在
        boolean isUser = false;
        ArrayList<User> users = User.getUsers();

        for (User u : users) {
            if (user.getUser().equals(u.getUser())
                    && strs[1].equals(u.getPassword())) {
                isUser = true;
                user = u;// 整个user都赋值过去
                break;
            }
        }
        if (isUser) {// 是我们的用户
            response("230 User logged in.");
        } else {// 非法用户
            response("530 Not logged in,you account is wrong.");
        }
    }

    /**
     * post 请求命令:
     */
    private void commandPORT() {
        String[] temp = strs[1].split(",");
        remoteHost = temp[0] + "." + temp[1] + "." + temp[2] + "." + temp[3];
        String port1 = null;
        String port2 = null;
        if (temp.length == 6) {
            port1 = temp[4];
            port2 = temp[5];
        } else {
            port1 = "0";
            port2 = temp[4];
        }
        remotePort = Integer.parseInt(port1) * 256 + Integer.parseInt(port2);
        response("200 PORT command successful.");
    }

    /**
     * List 命令：显示所有的文件
     */
    private void commandList() {
        response("150 Data connection already open; Transfer starting.");
        OutputStreamWriter dStream = null;
        BufferedWriter dWriter = null;
        try {

            dSocket = new Socket(remoteHost, remotePort);

            dStream = new OutputStreamWriter(dSocket.getOutputStream(), "gb2312");
            dWriter = new BufferedWriter(dStream);

            String tab = "     ";// 5个空格
            String fMess="";// 文件信息
            // 要输出的数据
//            File file = new File(user.getWorkDir());
//            File[] files = file.listFiles();

//
//            for (File f : files) {
//                fMess = TimeDealer.timeFormat(f.lastModified())// 时间
//                        + tab // 格式
//                        + (f.isFile() ? tab : "<DIR>") + tab // 格式
//                        + f.getName();
//                printStr(dWriter, fMess);
//            }
            String fileName = "";
            String dir = LogUserService.class.getResource("/").getPath();
            dir += "file_member.properties";
            try {
                FileInputStream fis = new FileInputStream(dir);
                Properties properties=new Properties();
                properties.load(fis);
                Enumeration enumeration = properties.propertyNames();
                while(enumeration.hasMoreElements()) {
                    String key=(String)enumeration.nextElement();
                    fMess = properties.getProperty(key) + tab + tab + key;
                    printStr(dWriter,fMess);
                }
            } catch (Exception e) {
                System.out.println(e);
            }

        } catch (UnknownHostException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                dWriter.close();
                dStream.close();
                dSocket.close();
                dSocket = null;
            } catch (IOException e) {
                e.printStackTrace();
            }

        }
        response("226 transfer complete");

    }

    private void delFile(String fileName) {
        //删除本地文件
        String fileAllPath=user.getWorkDir()+File.separator + fileName;
        File fileAllFile=new File(fileAllPath);
        String [] fileTemp=fileAllFile.list();
        for(String temp:fileTemp) {
            new File(fileAllPath+File.separator+temp).delete();
        }
        //删除服务器的内容
        for(int j=0;j<SERVICES.size();j++) {
            String url = SERVICES.get(j);//拿出一个服务器地址
            url += "/deleteFile/"+fileName.split("\\.")+"_"+j+".part";
            HttpClient client = HttpClients.createDefault();
            HttpPost post = new HttpPost(url);
            HttpResponse response = null;
            try {
                response = client.execute(post);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }




    private  void delFolder(String folderPath) {
        try {
            String fileName=folderPath;
            folderPath= user.getWorkDir()+ File.separator+folderPath;
            delAllFile(folderPath); //删除完里面所有内容
            String filePath = folderPath;
            filePath = filePath.toString();
            java.io.File myFilePath = new java.io.File(filePath);
            myFilePath.delete(); //删除空文件夹
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    private  boolean delAllFile(String path) {
        boolean flag = false;

        File file = new File(path);
        if (!file.exists()) {
            return flag;
        }
        if (!file.isDirectory()) {
            return flag;
        }
        String[] tempList = file.list();
        File temp = null;
        for (int i = 0; i < tempList.length; i++) {
            if (path.endsWith(File.separator)) {
                temp = new File(path + tempList[i]);
            } else {
                temp = new File(path + File.separator + tempList[i]);
            }
            if (temp.isFile()) {
                temp.delete();

            }
            if (temp.isDirectory()) {
                delAllFile(path + File.separator + tempList[i]);//先删除文件夹里面的文件
                delFolder(path +File.separator + tempList[i]);//再删除空文件夹
                flag = true;
            }
        }
        return flag;
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
            if (null != in) {
                if (null != in) {
                    IOUtils.closeQuietly(in);
                }
            }
            return value;
        }
    }

}
