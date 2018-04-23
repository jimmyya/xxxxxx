package com.chen.web;

import com.chen.service.server.LogUserService;
import org.apache.commons.codec.digest.DigestUtils;
import org.junit.Test;

import java.io.*;

import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.channels.FileChannel;
import java.util.Properties;
import org.apache.commons.io.IOUtils;
/**
 * Created by CHEN on 2016/12/18.
 */
public class FileTest {
    @Test
    public void main() {
        FileInputStream fis=null;

        String dir =  "E:\\project\\my_hadoop\\src\\main\\resources\\file_name.properties";
        try {
            fis = new FileInputStream(dir);

        Properties properties=new Properties();
        properties.load(fis);
            FileOutputStream fos= new FileOutputStream(dir);


//        String md5 = DigestUtils.md5Hex(IOUtils.toByteArray(fis));
//        IOUtils.closeQuietly(fis);
//        System.out.println("MD5:"+md5);
        properties.remove("f");
        properties.store(fos,"11");
        fis.close();
        fis.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
