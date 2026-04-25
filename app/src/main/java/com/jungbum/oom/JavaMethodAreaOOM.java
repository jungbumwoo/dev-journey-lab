package com.jungbum.oom;

import java.lang.reflect.Method;

import net.sf.cglib.proxy.Enhancer;
import net.sf.cglib.proxy.MethodInterceptor;
import net.sf.cglib.proxy.MethodProxy;

/**
 * VM 매개변수：(JDK 7 이하) -XX:PermSize=10M -XX:MaxPermSize=10M
 * VM 매개변수：(JDK 8 이상) -XX:MetaspaceSize=10M -XX:MaxMetaspaceSize=10M
 *
 * @author zzm
 */
public class JavaMethodAreaOOM {

    public static void main(String[] args) {
        while (true) {
            Enhancer enhancer = new Enhancer();
            enhancer.setSuperclass(OOMObject.class);
            enhancer.setUseCache(false);
            enhancer.setCallback(new MethodInterceptor() {
                public Object intercept(Object obj, Method method, Object[] args, MethodProxy proxy) throws Throwable {
                    return proxy.invokeSuper(obj, args);
                }
            });
            enhancer.create();
        }
    }

    static class OOMObject {
    }
}

/*
 * @author jb
 * ---
 * javac app/src/main/java/com/jungbum/oom/JavaVMStackOOM.java
 * java -XX:MetaspaceSize=10M -XX:MaxMetaspaceSize=10M -cp app/src/main/java com.jungbum.oom.JavaVMStackOOM
 * 실행 결과:
 *
 * Exception in thread "main" java.lang.OutOfMemoryError: unable to create new native thread
 *         at java.lang.Thread.start0(Native Method)
 *         at java.lang.Thread.start(Thread.java:719)
 *         at com.jungbum.oom.JavaVMStackOOM.stackLeakByThread(JavaVMStackOOM.java:23)
 *         at com.jungbum.oom.JavaVMStackOOM.main(JavaVMStackOOM.java:29)
* */