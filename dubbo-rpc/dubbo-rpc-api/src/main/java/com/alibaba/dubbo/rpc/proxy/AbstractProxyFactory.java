/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.alibaba.dubbo.rpc.proxy;

import com.alibaba.dubbo.common.Constants;
import com.alibaba.dubbo.common.utils.ReflectUtils;
import com.alibaba.dubbo.rpc.Invoker;
import com.alibaba.dubbo.rpc.ProxyFactory;
import com.alibaba.dubbo.rpc.RpcException;
import com.alibaba.dubbo.rpc.service.EchoService;

/**
 * AbstractProxyFactory
 */
public abstract class AbstractProxyFactory implements ProxyFactory {

    /**
     *
     * @param invoker   Protocol生成的Invoker
     * @param <T>
     * @return
     * @throws RpcException
     */
    public <T> T getProxy(Invoker<T> invoker) throws RpcException {
        Class<?>[] interfaces = null;
        // 可以配置代理类需要实现的接口
        String config = invoker.getUrl().getParameter("interfaces");
        if (config != null && config.length() > 0) {
            String[] types = Constants.COMMA_SPLIT_PATTERN.split(config);
            if (types != null && types.length > 0) {
                interfaces = new Class<?>[types.length + 2];
                interfaces[0] = invoker.getInterface();
                interfaces[1] = EchoService.class;
                for (int i = 0; i < types.length; i++) {
                    interfaces[i + 1] = ReflectUtils.forName(types[i]);
                }
            }
        }
        if (interfaces == null) {
            interfaces = new Class<?>[]{invoker.getInterface(), EchoService.class};
        }
        // interfaces，默认加上EchoService接口，在dubbo里所有的服务提供Service都会生成
        // 代理实现EchoService，用于回声测试
        // interfaces是代理类需要实现的接口
        return getProxy(invoker, interfaces);
    }

    /**
     * 创建代理
     *
     * @param invoker   Protocol生成的Invoker
     * @param types 接口类型，动态生成的proxy需要实现的接口
     * @param <T>
     * @return
     */
    public abstract <T> T getProxy(Invoker<T> invoker, Class<?>[] types);

}