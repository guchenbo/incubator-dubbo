package cre.dubbo;

import com.alibaba.dubbo.common.extension.ExtensionLoader;
import com.alibaba.dubbo.rpc.ProxyFactory;

/**
 * @Author guchenbo
 * @Date 2018/9/16.
 */
public class ExtensionApi {

    public static void main(String[] args) {
        ExtensionLoader<ProxyFactory> extensionLoader = ExtensionLoader.getExtensionLoader(ProxyFactory.class);

        System.out.println(extensionLoader.getSupportedExtensions());
        System.out.println(extensionLoader.getExtension("stub"));
    }
}
