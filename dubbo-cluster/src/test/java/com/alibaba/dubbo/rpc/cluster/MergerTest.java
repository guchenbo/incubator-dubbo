package com.alibaba.dubbo.rpc.cluster;

import com.alibaba.dubbo.common.extension.ExtensionLoader;

/**
 * @Author guchenbo
 * @Date 2018/9/12.
 */
public class MergerTest {


    public static void main(String[] args) {
        ExtensionLoader extensionLoader = ExtensionLoader.getExtensionLoader(Merger.class);
//        extensionLoader.getAdaptiveExtension();

        System.out.println(extensionLoader.getSupportedExtensions());
    }
}