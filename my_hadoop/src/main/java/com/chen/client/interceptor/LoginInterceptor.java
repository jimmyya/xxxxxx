package com.chen.client.interceptor;

import java.lang.reflect.Method;

import com.chen.client.model.UserModel;

import net.sf.cglib.proxy.MethodInterceptor;
import net.sf.cglib.proxy.MethodProxy;

public class LoginInterceptor implements MethodInterceptor {
    @Override
    public Object intercept(Object obj, Method method, Object[] arg,
                            MethodProxy proxy) throws Throwable {

        boolean flag = false;
        Object object = null;
        for (Object o : arg) {
            if (o instanceof UserModel) {
                flag = ((UserModel) o).isLogin();
            }
        }
        if ("loginUser".equals(method.getName())) {
            flag = true;
        }
        if (flag) {
            object = proxy.invokeSuper(obj, arg);
        } else {
            System.out.println("请先登录");
        }
        return object;
    }
}
