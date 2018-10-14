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

package com.alibaba.dubbo.registry.zookeeper;

import com.alibaba.dubbo.common.Constants;
import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.common.logger.Logger;
import com.alibaba.dubbo.common.logger.LoggerFactory;
import com.alibaba.dubbo.common.utils.ConcurrentHashSet;
import com.alibaba.dubbo.common.utils.UrlUtils;
import com.alibaba.dubbo.registry.NotifyListener;
import com.alibaba.dubbo.registry.support.FailbackRegistry;
import com.alibaba.dubbo.remoting.zookeeper.ChildListener;
import com.alibaba.dubbo.remoting.zookeeper.StateListener;
import com.alibaba.dubbo.remoting.zookeeper.ZookeeperClient;
import com.alibaba.dubbo.remoting.zookeeper.ZookeeperTransporter;
import com.alibaba.dubbo.rpc.RpcException;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * ZookeeperRegistry
 */
public class ZookeeperRegistry extends FailbackRegistry {

    private final static Logger logger = LoggerFactory.getLogger(ZookeeperRegistry.class);

    private final static int DEFAULT_ZOOKEEPER_PORT = 2181;

    private final static String DEFAULT_ROOT = "dubbo";

    private final String root;

    private final Set<String> anyServices = new ConcurrentHashSet<String>();

    private final ConcurrentMap<URL, ConcurrentMap<NotifyListener, ChildListener>> zkListeners = new ConcurrentHashMap<URL, ConcurrentMap<NotifyListener, ChildListener>>();

    private final ZookeeperClient zkClient;

    public ZookeeperRegistry(URL url, ZookeeperTransporter zookeeperTransporter) {
        super(url);
        if (url.isAnyHost()) {
            throw new IllegalStateException("registry address == null");
        }
        // group作为zookeeper的root节点名称，默认是dubbo
        String group = url.getParameter(Constants.GROUP_KEY, DEFAULT_ROOT);
        if (!group.startsWith(Constants.PATH_SEPARATOR)) {
            group = Constants.PATH_SEPARATOR + group;
        }
        this.root = group;
        zkClient = zookeeperTransporter.connect(url);
        // 增加监听器，使用匿名类，用于自动恢复
        zkClient.addStateListener(new StateListener() {
            public void stateChanged(int state) {
                if (state == RECONNECTED) {
                    try {
                        // 状态为重连接，进行恢复逻辑，内存中保存注册的信息，和订阅信息
                        recover();
                    } catch (Exception e) {
                        logger.error(e.getMessage(), e);
                    }
                }
            }
        });
    }

    static String appendDefaultPort(String address) {
        if (address != null && address.length() > 0) {
            int i = address.indexOf(':');
            if (i < 0) {
                return address + ":" + DEFAULT_ZOOKEEPER_PORT;
            } else if (Integer.parseInt(address.substring(i + 1)) == 0) {
                return address.substring(0, i + 1) + DEFAULT_ZOOKEEPER_PORT;
            }
        }
        return address;
    }

    public boolean isAvailable() {
        return zkClient.isConnected();
    }

    public void destroy() {
        super.destroy();
        try {
            zkClient.close();
        } catch (Exception e) {
            logger.warn("Failed to close zookeeper client " + getUrl() + ", cause: " + e.getMessage(), e);
        }
    }

    /**
     * zk创建节点
     *
     * @param url
     */
    protected void doRegister(URL url) {
        try {
            // dynamic=false 表示持久化存储
            zkClient.create(toUrlPath(url), url.getParameter(Constants.DYNAMIC_KEY, true));
        } catch (Throwable e) {
            throw new RpcException(
                            "Failed to register " + url + " to zookeeper " + getUrl() + ", cause: " + e.getMessage(),
                            e);
        }
    }

    protected void doUnregister(URL url) {
        try {
            zkClient.delete(toUrlPath(url));
        } catch (Throwable e) {
            throw new RpcException(
                            "Failed to unregister " + url + " to zookeeper " + getUrl() + ", cause: " + e.getMessage(),
                            e);
        }
    }

    protected void doSubscribe(final URL url, final NotifyListener listener) {
        try {
            if (Constants.ANY_VALUE.equals(url.getServiceInterface())) {
                // 如果是*，订阅所有Service节点，比如监控中心的订阅
                String root = toRootPath();
                ConcurrentMap<NotifyListener, ChildListener> listeners = zkListeners.get(url);
                if (listeners == null) {
                    zkListeners.putIfAbsent(url, new ConcurrentHashMap<NotifyListener, ChildListener>());
                    listeners = zkListeners.get(url);
                }
                // 创建zookeeper监听器
                ChildListener zkListener = listeners.get(listener);
                if (zkListener == null) {
                    listeners.putIfAbsent(listener, new ChildListener() {
                        public void childChanged(String parentPath, List<String> currentChilds) {
                            for (String child : currentChilds) {
                                child = URL.decode(child);
                                // child是Service类型节点
                                // service没有订阅过才订阅，不重复订阅
                                if (!anyServices.contains(child)) {
                                    anyServices.add(child);
                                    // 有Service变更时，对指定Service节点发起订阅
                                    subscribe(url.setPath(child)
                                                    .addParameters(Constants.INTERFACE_KEY, child, Constants.CHECK_KEY,
                                                                    String.valueOf(false)), listener);
                                }
                            }
                        }
                    });
                    zkListener = listeners.get(listener);
                }
                // 以防万一先创建节点，不存在才会创建，false表示持久化，这个path是Category节点
                zkClient.create(root, false);
                // 每次重新绑定监听器，获取子节点信息
                // 是在root节点上新增监听器，所以返回的是所有Service类型的节点信息
                List<String> services = zkClient.addChildListener(root, zkListener);

                // 循环对每个Service节点发起订阅
                if (services != null && services.size() > 0) {
                    for (String service : services) {
                        service = URL.decode(service);
                        anyServices.add(service);
                        subscribe(url.setPath(service)
                                        .addParameters(Constants.INTERFACE_KEY, service, Constants.CHECK_KEY,
                                                        String.valueOf(false)), listener);
                    }
                }
            } else {
                // 订阅指定Service节点
                // 从url中判断需要订阅的分组信息，获取每个分组下面符合条件的url数组
                List<URL> urls = new ArrayList<URL>();
                for (String path : toCategoriesPath(url)) {
                    ConcurrentMap<NotifyListener, ChildListener> listeners = zkListeners.get(url);
                    if (listeners == null) {
                        zkListeners.putIfAbsent(url, new ConcurrentHashMap<NotifyListener, ChildListener>());
                        listeners = zkListeners.get(url);
                    }
                    // 创建zookeeper监听器
                    ChildListener zkListener = listeners.get(listener);
                    if (zkListener == null) {
                        listeners.putIfAbsent(listener, new ChildListener() {
                            // 某个分组path下面的字节点发生变更，就会触发这个方法，
                            // 从而回调触发NotifyListener#notify()，
                            // 这个是增量数据
                            // 什么时候数据会变更呢，比如新增一个服务提供者，或者某个服务提供者断掉，Providers
                            // 节点下面的信息发生了变更，会将变更之后的节点信息触发回调
                            public void childChanged(String parentPath, List<String> currentChilds) {
                                ZookeeperRegistry.this
                                                .notify(url, listener, toUrlsWithEmpty(url, parentPath, currentChilds));
                            }
                        });
                        zkListener = listeners.get(listener);
                    }
                    // 以防万一先创建节点，不存在才会创建，false表示持久化，这个path是Category节点
                    zkClient.create(path, false);
                    // 每次重新绑定监听器
                    List<String> children = zkClient.addChildListener(path, zkListener);
                    if (children != null) {
                        urls.addAll(toUrlsWithEmpty(url, path, children));
                    }
                }
                // 首次订阅时手动触发，获得全量urls数据，触发回调NotifyListener#notify()方法
                // 这个是全量数据
                notify(url, listener, urls);
            }
        } catch (Throwable e) {
            throw new RpcException(
                            "Failed to subscribe " + url + " to zookeeper " + getUrl() + ", cause: " + e.getMessage(),
                            e);
        }
    }

    protected void doUnsubscribe(URL url, NotifyListener listener) {
        ConcurrentMap<NotifyListener, ChildListener> listeners = zkListeners.get(url);
        if (listeners != null) {
            ChildListener zkListener = listeners.get(listener);
            if (zkListener != null) {
                // TODO: 2018/9/15 这里的path，为什么不是toCategoryUrl()
                zkClient.removeChildListener(toUrlPath(url), zkListener);
            }
        }
    }

    public List<URL> lookup(URL url) {
        if (url == null) {
            throw new IllegalArgumentException("lookup url == null");
        }
        try {
            List<String> providers = new ArrayList<String>();
            for (String path : toCategoriesPath(url)) {
                List<String> children = zkClient.getChildren(path);
                if (children != null) {
                    providers.addAll(children);
                }
            }
            return toUrlsWithoutEmpty(url, providers);
        } catch (Throwable e) {
            throw new RpcException(
                            "Failed to lookup " + url + " from zookeeper " + getUrl() + ", cause: " + e.getMessage(),
                            e);
        }
    }

    private String toRootDir() {
        if (root.equals(Constants.PATH_SEPARATOR)) {
            return root;
        }
        return root + Constants.PATH_SEPARATOR;
    }

    private String toRootPath() {
        return root;
    }

    private String toServicePath(URL url) {
        String name = url.getServiceInterface();
        if (Constants.ANY_VALUE.equals(name)) {
            return toRootPath();
        }
        return toRootDir() + URL.encode(name);
    }

    /**
     * 返回分组的数组，例如["providers","consumers"]，在配置category，可以是多个","号分隔
     * 如果category=*  表示订阅全部分组
     *
     *
     * @param url
     * @return  ["/Root/Service/category"]
     */
    private String[] toCategoriesPath(URL url) {
        String[] categroies;
        if (Constants.ANY_VALUE.equals(url.getParameter(Constants.CATEGORY_KEY))) {
            categroies = new String[] { Constants.PROVIDERS_CATEGORY, Constants.CONSUMERS_CATEGORY,
                            Constants.ROUTERS_CATEGORY, Constants.CONFIGURATORS_CATEGORY };
        } else {
            categroies = url.getParameter(Constants.CATEGORY_KEY, new String[] { Constants.DEFAULT_CATEGORY });
        }
        String[] paths = new String[categroies.length];
        for (int i = 0; i < categroies.length; i++) {
            paths[i] = toServicePath(url) + Constants.PATH_SEPARATOR + categroies[i];
        }
        return paths;
    }

    private String toCategoryPath(URL url) {
        return toServicePath(url) + Constants.PATH_SEPARATOR + url
                        .getParameter(Constants.CATEGORY_KEY, Constants.DEFAULT_CATEGORY);
    }

    /**
     * 构建url路径：Root / Service / Category / Url
     * 例如：/dubbo/com.alibaba.dubbo.registry.RegistryService/providers,consumers/zookeeper%3A%2F%2F127.0.0.1%3A9090%2Fcom.alibaba.dubbo.registry.RegistryService%3Fapplication%3Ddemo%26backup%3D127.0.0.2%3A9090%26category%3Dproviders%2Cconsumers%26dubbo%3D2.5.3%26interface%3Dcom.alibaba.dubbo.registry.RegistryService%26pid%3D9545%26timestamp%3D1536987127248
     *
     * @param url
     * @return
     */
    private String toUrlPath(URL url) {
        return toCategoryPath(url) + Constants.PATH_SEPARATOR + URL.encode(url.toFullString());
    }

    /**
     * 获得 providers 中和 consumer 匹配的url数组
     *
     * @param consumer
     * @param providers
     * @return
     */
    private List<URL> toUrlsWithoutEmpty(URL consumer, List<String> providers) {
        List<URL> urls = new ArrayList<URL>();
        if (providers != null && providers.size() > 0) {
            for (String provider : providers) {
                provider = URL.decode(provider);
                if (provider.contains("://")) {
                    URL url = URL.valueOf(provider);
                    if (UrlUtils.isMatch(consumer, url)) {
                        urls.add(url);
                    }
                }
            }
        }
        return urls;
    }

    /**
     * 获得 providers 中和 consumer 匹配的url数组，如果为空，新增一个empty://的url返回
     *
     * @param consumer
     * @param path
     * @param providers
     * @return
     */
    private List<URL> toUrlsWithEmpty(URL consumer, String path, List<String> providers) {
        List<URL> urls = toUrlsWithoutEmpty(consumer, providers);
        if (urls == null || urls.isEmpty()) {
            // empty://
            int i = path.lastIndexOf('/');
            String category = i < 0 ? path : path.substring(i + 1);
            URL empty = consumer.setProtocol(Constants.EMPTY_PROTOCOL).addParameter(Constants.CATEGORY_KEY, category);
            urls.add(empty);
        }
        return urls;
    }

}