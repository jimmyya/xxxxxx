package com.chen.web;
import com.chen.dao.FileMapper;
import com.chen.model.FileModel;
import com.chen.service.utils.MybatisUtil;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
public class Test {
    static SqlSessionFactory sqlSessionFactory = null;
    static {
        sqlSessionFactory = MybatisUtil.getSqlSessionFactory();
    }

    public static void main(String[] args) {
        System.out.println("12345".substring("12345".indexOf("12")+"12".length()));

    }

    public static void testAdd() {
        SqlSession sqlSession = sqlSessionFactory.openSession();
        try {
            FileMapper fileMapper = sqlSession.getMapper(FileMapper.class);
            FileModel fileModel = new FileModel();
            fileModel.setId(1);
            fileModel.setMd5("2214");
            fileModel.setName("12");
            fileModel.setParentName(2);
            fileModel.setTime("222");
            fileMapper.insertFileModel(fileModel);
            sqlSession.commit();// 这里一定要提交，不然数据进不去数据库中
        } finally {
            sqlSession.close();
        }
    }

    public static void getFile() {
        SqlSession sqlSession = sqlSessionFactory.openSession();
        try {
            FileMapper fileMapper = sqlSession.getMapper(FileMapper.class);
            FileModel fileModel = new FileModel();
            fileModel.setId(1);
            fileModel.setName("12");
            fileModel.setMd5("2214");
            System.out.println(fileMapper.queryFileModel(fileModel).get(0));
        } finally {
            sqlSession.close();
        }
    }



}
