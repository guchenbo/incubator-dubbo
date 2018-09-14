package com.alibaba.dubbo.remoting.exchange;

import com.alibaba.dubbo.common.extension.ExtensionLoader;

import static org.junit.Assert.*;

/**
 * @Author guchenbo
 * @Date 2018/9/12.
 */
public class ExchangerTest {

    public static void main(String[] args) {
        ExtensionLoader extensionLoader = ExtensionLoader.getExtensionLoader(Exchanger.class);
        extensionLoader.getAdaptiveExtension();

        System.out.println(extensionLoader.getSupportedExtensions());
    }

}