package cre.dubbo;

import com.alibaba.dubbo.common.bytecode.Proxy;
import com.alibaba.dubbo.demo.DemoService;

/**
 * @Author guchenbo
 * @Date 2018/9/16.
 */
public class ProxyApi {

    public static void main(String[] args) {
        Class[] types = new Class[] { DemoService.class };
        Proxy proxy = Proxy.getProxy(types);
        proxy.newInstance();
    }
}
