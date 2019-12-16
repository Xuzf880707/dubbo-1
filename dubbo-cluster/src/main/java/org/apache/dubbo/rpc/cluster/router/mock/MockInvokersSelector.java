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
package org.apache.dubbo.rpc.cluster.router.mock;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.utils.CollectionUtils;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.RpcException;
import org.apache.dubbo.rpc.cluster.router.AbstractRouter;

import java.util.ArrayList;
import java.util.List;

import static org.apache.dubbo.rpc.cluster.Constants.INVOCATION_NEED_MOCK;
import static org.apache.dubbo.rpc.cluster.Constants.MOCK_PROTOCOL;

/**
 * A specific Router designed to realize mock feature.
 * If a request is configured to use mock, then this router guarantees that only the invokers with protocol MOCK appear in final the invoker list, all other invokers will be excluded.
 *
 * 从Attachments解析invocation.need.mock属性，
 * 判断此次调用是否是一个mock调用：
 *      是：则找出protocol协议是mock的invoker
 *      否：则找出protocol协议是mock的invoker
 */
public class MockInvokersSelector extends AbstractRouter {

    public static final String NAME = "MOCK_ROUTER";
    private static final int MOCK_INVOKERS_DEFAULT_PRIORITY = Integer.MIN_VALUE;

    public MockInvokersSelector() {
        this.priority = MOCK_INVOKERS_DEFAULT_PRIORITY;
    }

    @Override
    public <T> List<Invoker<T>> route(final List<Invoker<T>> invokers,
                                      URL url, final Invocation invocation) throws RpcException {
        if (CollectionUtils.isEmpty(invokers)) {
            return invokers;
        }

        if (invocation.getAttachments() == null) {
            return getNormalInvokers(invokers);
        } else {
            //从attachments中获得invocation.need.mock属性值，判断它是不是一个 mock 调用
            String value = (String) invocation.getAttachments().get(INVOCATION_NEED_MOCK);
            if (value == null) {
                return getNormalInvokers(invokers);
            } else if (Boolean.TRUE.toString().equalsIgnoreCase(value)) {
                return getMockedInvokers(invokers);
            }
        }
        return invokers;
    }

    /***
     * 遍历所有的服务提供者类标，返回mock协议的服务提供者列表
     * @param invokers
     * @param <T>
     * @return
     */
    private <T> List<Invoker<T>> getMockedInvokers(final List<Invoker<T>> invokers) {
        if (!hasMockProviders(invokers)) {//interface org.apache.dubbo.demo.MockService -> dubbo://192.168.0.104:20880/org.apache.dubbo.demo.MockService?anyhost=true&bean.name=org.apache.dubbo.demo.MockService&check=false&deprecated=false&dubbo=2.0.2&dynamic=true&generic=false&init=false&interface=org.apache.dubbo.demo.MockService&lazy=false&methods=sayHello&mock=force%3Aorg.apache.dubbo.demo.consumer.MockServiceMock&pid=73942&register.ip=192.168.0.104&release=&remote.application=&side=consumer&sticky=false&timestamp=1575936295944
            return null;
        }
        List<Invoker<T>> sInvokers = new ArrayList<Invoker<T>>(1);
        for (Invoker<T> invoker : invokers) {
            if (invoker.getUrl().getProtocol().equals(MOCK_PROTOCOL)) {
                sInvokers.add(invoker);
            }
        }
        return sInvokers;
    }

    /**
     *
     * @param invokers
     * @param <T>
     * @return
     *      获得非mock的服务提供者(两个分支可以合在一块)
     *      1、遍历提供者列表，如果没有mock协议的提供者列表，则返回全部的服务提供者列表
     *      2、如果有mock协议的提供者列表，只返回非mock的服务提供者列表
     */
    private <T> List<Invoker<T>> getNormalInvokers(final List<Invoker<T>> invokers) {
        if (!hasMockProviders(invokers)) {
            return invokers;
        } else {
            List<Invoker<T>> sInvokers = new ArrayList<Invoker<T>>(invokers.size());
            for (Invoker<T> invoker : invokers) {
                if (!invoker.getUrl().getProtocol().equals(MOCK_PROTOCOL)) {
                    sInvokers.add(invoker);
                }
            }
            return sInvokers;
        }
    }

    /***
     * 判断服务提供者列表里，是否存在mock协议的提供者，并返回false/true
     * @param invokers
     * @param <T>
     * @return
     */
    private <T> boolean hasMockProviders(final List<Invoker<T>> invokers) {
        boolean hasMockProvider = false;
        for (Invoker<T> invoker : invokers) {
            if (invoker.getUrl().getProtocol().equals(MOCK_PROTOCOL)) {
                hasMockProvider = true;
                break;
            }
        }
        return hasMockProvider;
    }

}
