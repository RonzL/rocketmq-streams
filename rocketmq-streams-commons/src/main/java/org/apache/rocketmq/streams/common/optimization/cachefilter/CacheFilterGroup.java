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

package org.apache.rocketmq.streams.common.optimization.cachefilter;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import org.apache.rocketmq.streams.common.cache.compress.BitSetCache;
import org.apache.rocketmq.streams.common.context.AbstractContext;
import org.apache.rocketmq.streams.common.context.IMessage;
import org.apache.rocketmq.streams.common.utils.MapKeyUtil;

/**
 * group by var name
 */
public class CacheFilterGroup {
    //key: varName; value :list IOptimizationExpression
    protected String name;//mutli FilterOptimization shared cachefilter，need name
    protected String varName;
    protected List<ICacheFilter> expressionList=new ArrayList<>();
    protected BitSetCache cache;
    public CacheFilterGroup(String name,String varName,BitSetCache cache){
        this.name=name;
        this.varName=varName;
        this.cache=cache;
    }

    public void addOptimizationExpression(ICacheFilter expression){
        this.expressionList.add(expression);
    }

    public static AtomicLong totalCount=new AtomicLong(0);
    public static AtomicLong matchCount=new AtomicLong(0);
    public void execute(IMessage message, AbstractContext context){
        totalCount.incrementAndGet();
        String key= MapKeyUtil.createKey(name,message.getMessageBody().getString(varName));
        BitSetCache.BitSet bitSet = cache.get(key);
        if(bitSet==null){
            bitSet=cache.createBitSet();
            for(int i=0;i<expressionList.size();i++){
                ICacheFilter cacheFilter=expressionList.get(i);
                boolean isMatch=cacheFilter.executeOrigExpression(message,context);
                if(isMatch){
                    bitSet.set(i);
                }
                context.put(cacheFilter,isMatch);
            }
            cache.put(key,bitSet);
        }else {
            matchCount.incrementAndGet();
            for(int i=0;i<expressionList.size();i++){
                ICacheFilter cacheFilter=expressionList.get(i);
                boolean isMatch=bitSet.get(i);
                context.put(cacheFilter,isMatch);
            }
        }
//        if(totalCount.get()>0&&totalCount.get()%100==0){
//
//            System.out.println("filter rate is "+(double)matchCount.get()/(double)totalCount.get());
//        }
    }

    public int getSize(){
        return expressionList.size();
    }
}
