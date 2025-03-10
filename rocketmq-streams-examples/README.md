## rocketmq-streams-examples

### 1、File source example
逐行读取文件数据，并打印出来。
```java
public class FileSourceExample {
    public static void main(String[] args) {
        DataStreamSource source = StreamBuilder.dataStream("namespace", "pipeline");
        source.fromFile("data.txt", false)
                .map(message -> message)
                .toPrint(1)
                .start();
    }
}

```


### 2、分时间段，统计分组中某字段的和

#### 2.1 安装 Apache RocketMQ
可以参考[Apache RocketMQ 搭建文档](https://rocketmq.apache.org/docs/quick-start/)

#### 2.2 源数据
[源数据](./../rocketmq-streams-examples/src/main/resources/data.txt)
```xml
{"InFlow":"1","ProjectName":"ProjectName-0","LogStore":"LogStore-0","OutFlow":"0"}
{"InFlow":"2","ProjectName":"ProjectName-1","LogStore":"LogStore-1","OutFlow":"1"}
{"InFlow":"3","ProjectName":"ProjectName-2","LogStore":"LogStore-2","OutFlow":"2"}
{"InFlow":"4","ProjectName":"ProjectName-0","LogStore":"LogStore-0","OutFlow":"3"}
{"InFlow":"5","ProjectName":"ProjectName-1","LogStore":"LogStore-1","OutFlow":"4"}
{"InFlow":"6","ProjectName":"ProjectName-2","LogStore":"LogStore-2","OutFlow":"5"}
{"InFlow":"7","ProjectName":"ProjectName-0","LogStore":"LogStore-0","OutFlow":"6"}
{"InFlow":"8","ProjectName":"ProjectName-1","LogStore":"LogStore-1","OutFlow":"7"}
{"InFlow":"9","ProjectName":"ProjectName-2","LogStore":"LogStore-2","OutFlow":"8"}
{"InFlow":"10","ProjectName":"ProjectName-0","LogStore":"LogStore-0","OutFlow":"9"}
```

#### 2.3 代码示例

[代码示例](./../rocketmq-streams-examples/src/main/java/org/apache/rocketmq/streams/examples/rocketmqsource/RocketmqWindowTest.java)


#### 2.4 结果说明
这个例子中，使用 rocketmq-streams 消费 rocketmq 中的数据，并按照 ProjectName 和 LogStore 两个字段联合分组统计，两个字段的值相同，分为一组。
分别统计每组的InFlow和OutFlow两字段累计和。

data.text数据运行的结果部分如下：

```xml
"InFlow":22,"total":4,"ProjectName":"ProjectName-0","LogStore":"LogStore-0","OutFlow":18
"InFlow":18,"total":3,"ProjectName":"ProjectName-2","LogStore":"LogStore-2","OutFlow":15
"InFlow":15,"total":3,"ProjectName":"ProjectName-1","LogStore":"LogStore-1","OutFlow":12
```
可见"ProjectName":"ProjectName-0","LogStore":"LogStore-0"分组公有4条数据，"ProjectName":"ProjectName-2","LogStore":"LogStore-2"，3条数据。
"ProjectName":"ProjectName-1","LogStore":"LogStore-1"分组3条数据，总共10条数据。结果与源数据一致。

### 3、网页点击统计
#### 3.1、数据说明
原始数据为resources路径下的[pageClickData.txt](./../rocketmq-streams-examples/src/main/resources/pageClickData.txt)

第一列是用户id，第二列是用户点击时间，最后一列是网页地址
```xml
{"userId":"1","eventTime":"1631700000000","method":"GET","url":"page-1"}
{"userId":"2","eventTime":"1631700030000","method":"POST","url":"page-2"}
{"userId":"3","eventTime":"1631700040000","method":"GET","url":"page-3"}
{"userId":"1","eventTime":"1631700050000","method":"DELETE","url":"page-2"}
{"userId":"1","eventTime":"1631700060000","method":"DELETE","url":"page-2"}
{"userId":"2","eventTime":"1631700070000","method":"POST","url":"page-3"}
{"userId":"3","eventTime":"1631700080000","method":"GET","url":"page-1"}
{"userId":"1","eventTime":"1631700090000","method":"GET","url":"page-2"}
{"userId":"2","eventTime":"1631700100000","method":"PUT","url":"page-3"}
{"userId":"4","eventTime":"1631700120000","method":"POST","url":"page-1"}
```

#### 3.1、统计某段时间窗口内用户点击网页次数
[代码示例](./../rocketmq-streams-examples/src/main/java/org/apache/rocketmq/streams/examples/pageclick/UsersDimension.java)

结果：
```xml
{"start_time":"2021-09-15 18:00:00","total":1,"windowInstanceId":"SPVGTV6DaXmxV5mGNzQixQ==","offset":53892061100000001,"end_time":"2021-09-15 18:01:00","userId":"2"}
{"start_time":"2021-09-15 18:00:00","total":1,"windowInstanceId":"dzAZ104qjUAwzTE6gbKSPA==","offset":53892061100000001,"end_time":"2021-09-15 18:01:00","userId":"3"}
{"start_time":"2021-09-15 18:00:00","total":2,"windowInstanceId":"wrTTyU5DiDkrAb6669Ig9w==","offset":53892061100000001,"end_time":"2021-09-15 18:01:00","userId":"1"}
{"start_time":"2021-09-15 18:01:00","total":1,"windowInstanceId":"vabkmx14xHsJ7G7w16vwug==","offset":53892121100000001,"end_time":"2021-09-15 18:02:00","userId":"3"}
{"start_time":"2021-09-15 18:01:00","total":2,"windowInstanceId":"YIgEKptN2Wf+Oq2m8sEcYw==","offset":53892121100000001,"end_time":"2021-09-15 18:02:00","userId":"2"}
{"start_time":"2021-09-15 18:01:00","total":2,"windowInstanceId":"iYKnwMYAzXFJYbO1KvDnng==","offset":53892121100000001,"end_time":"2021-09-15 18:02:00","userId":"1"}
{"start_time":"2021-09-15 18:02:00","total":1,"windowInstanceId":"HBojuU6/2F/6llkyefECxw==","offset":53892181100000001,"end_time":"2021-09-15 18:03:00","userId":"4"}
```

在时间范围 18:00:00- 18:01:00内：

|userId|点击次数|
|------|---|
|   1  | 2 |
|   2  | 1 |
|   3  | 1 |

在时间范围 18:01:00- 18:02:00内：

|userId|点击次数|
|------|---|
|   1  | 2 |
|   2  | 2 |
|   3  | 1 |

在时间范围 18:02:00- 18:03:00内：

|userId|点击次数|
|------|---|
|   4  | 1 | 

可查看原数据文件，eventTime为时间字段，简单检查后上述结果与预期相符合。

#### 3.2、统计某段时间窗口内，被点击次数最多的网页
[代码示例](./../rocketmq-streams-examples/src/main/java/org/apache/rocketmq/streams/examples/pageclick/PageDimension.java)

运行结果：
```xml
{"start_time":"2021-09-15 18:00:00","total":1,"windowInstanceId":"wrTTyU5DiDkrAb6669Ig9w==","offset":53892061100000001,"end_time":"2021-09-15 18:01:00","url":"page-1"}
{"start_time":"2021-09-15 18:00:00","total":2,"windowInstanceId":"seECZRcaQSRsET1rDc6ZAw==","offset":53892061100000001,"end_time":"2021-09-15 18:01:00","url":"page-2"}
{"start_time":"2021-09-15 18:00:00","total":1,"windowInstanceId":"dzAZ104qjUAwzTE6gbKSPA==","offset":53892061100000001,"end_time":"2021-09-15 18:01:00","url":"page-3"}
{"start_time":"2021-09-15 18:01:00","total":2,"windowInstanceId":"uCqvAeaLTYRnjQm8dCZOvw==","offset":53892121100000001,"end_time":"2021-09-15 18:02:00","url":"page-2"}
{"start_time":"2021-09-15 18:01:00","total":3,"windowInstanceId":"vabkmx14xHsJ7G7w16vwug==","offset":53892121100000001,"end_time":"2021-09-15 18:02:00","url":"page-3"}
{"start_time":"2021-09-15 18:02:00","total":1,"windowInstanceId":"NdgwYMT8azNMu55NUIvygg==","offset":53892181100000001,"end_time":"2021-09-15 18:03:00","url":"page-1"}

```
在时间窗口18:00:00 - 18:01:00 内，有4条数据；

在时间窗口18:01:00 - 18:02:00 内，有5条数据；

在时间窗口18:02:00 - 18:03:00 内，有1条数据；

分钟统计窗口内，被点击次数最多的网页.
得到上述数据后，需要按照窗口进行筛选最大值，需要再次计算。
代码：
```java
    public void findMax() {
        DataStreamSource source = StreamBuilder.dataStream("ns-1", "pl-1");
        source.fromFile("/home/result.txt", false)
        .map(message -> JSONObject.parseObject((String) message))
        .window(TumblingWindow.of(Time.seconds(5)))
        .groupBy("start_time","end_time")
        .max("total")
        .waterMark(1)
        .setLocalStorageOnly(true)
        .toDataSteam()
        .toPrint(1)
        .start();
   }

```
得到结果：
```xml
{"start_time":"2021-09-17 11:09:35","total":"2","windowInstanceId":"kRRpe2hPEQtEuTkfnXUaHg==","offset":54040181100000001,"end_time":"2021-09-17 11:09:40"}
{"start_time":"2021-09-17 11:09:35","total":"3","windowInstanceId":"kRRpe2hPEQtEuTkfnXUaHg==","offset":54040181100000002,"end_time":"2021-09-17 11:09:40"}
{"start_time":"2021-09-17 11:09:35","total":"1","windowInstanceId":"kRRpe2hPEQtEuTkfnXUaHg==","offset":54040181100000003,"end_time":"2021-09-17 11:09:40"}
```

可以得到三个窗口中网页点击次数最多分别是2次，1次，3次。
