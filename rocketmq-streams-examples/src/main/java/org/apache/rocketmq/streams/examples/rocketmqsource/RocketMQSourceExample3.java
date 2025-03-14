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

package org.apache.rocketmq.streams.examples.rocketmqsource;

import com.alibaba.fastjson.JSONObject;
import org.apache.rocketmq.streams.client.StreamBuilder;
import org.apache.rocketmq.streams.client.source.DataStreamSource;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.apache.rocketmq.streams.examples.rocketmqsource.Constant.NAMESRV_ADDRESS;
import static org.apache.rocketmq.streams.examples.rocketmqsource.Constant.RMQ_CONSUMER_GROUP_NAME;
import static org.apache.rocketmq.streams.examples.rocketmqsource.Constant.RMQ_TOPIC;

public class RocketMQSourceExample3 {
    /**
     * 1、before run this case, make sure some data has already been rocketmq.
     */
    public static void main(String[] args) {
        ProducerFromFile.produce("data.txt",NAMESRV_ADDRESS, RMQ_TOPIC);

        try {
            Thread.sleep(1000 * 3);
        } catch (InterruptedException e) {
        }

        System.out.println("begin streams code.");

        DataStreamSource source = StreamBuilder.dataStream("namespace", "pipeline");
        source.fromRocketmq(
                RMQ_TOPIC,
                RMQ_CONSUMER_GROUP_NAME,
                false,
                NAMESRV_ADDRESS)
                .forEach((message) -> {
                    System.out.println("forEach: " + message);
                })
                .map(message -> message)
                .filter((value) -> {
                    String messageValue = (String) value;
                    return !messageValue.contains("InFlow");
                })
                .flatMap((message) -> {
                    JSONObject jsonObject = JSONObject.parseObject((String) message);
                    Set<Map.Entry<String, Object>> entries = jsonObject.entrySet();

                    List<String> result = new ArrayList<>();

                    for (Map.Entry<String, Object> entry : entries) {
                        String str = entry.getKey() + ":" + entry.getValue();
                        result.add(str);
                    }
                    return result;
                })
                .toPrint(1)
                .start();

    }


}
