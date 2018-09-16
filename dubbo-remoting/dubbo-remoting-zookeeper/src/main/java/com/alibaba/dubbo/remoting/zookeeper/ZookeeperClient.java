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

package com.alibaba.dubbo.remoting.zookeeper;

import com.alibaba.dubbo.common.URL;

import java.util.List;

public interface ZookeeperClient {

    /**
     * 创建节点
     *
     * @param path
     * @param ephemeral 是否临时
     */
    void create(String path, boolean ephemeral);

    void delete(String path);

    List<String> getChildren(String path);

    /**
     * 为指定节点绑定监听器，并且返回子节点信息
     *
     * @param path
     * @param listener
     * @return
     */
    List<String> addChildListener(String path, ChildListener listener);

    void removeChildListener(String path, ChildListener listener);

    void addStateListener(StateListener listener);

    void removeStateListener(StateListener listener);

    boolean isConnected();

    void close();

    /**
     * 获得注册中心url
     *
     * @return
     */
    URL getUrl();

}
