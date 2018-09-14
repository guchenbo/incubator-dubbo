package com.alibaba.dubbo.rpc;

import com.alibaba.dubbo.common.extension.ExtensionLoader;

/**
 * @Author guchenbo
 * @Date 2018/9/12.
 */
public class ProtocolTest {

    public static void main(String[] args) {
        ExtensionLoader extensionLoader = ExtensionLoader.getExtensionLoader(Protocol.class);
        extensionLoader.getAdaptiveExtension();
    }
}