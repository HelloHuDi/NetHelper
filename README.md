# NetHelper

**整理一个专注于网络的工具库，主要用于监测网络状态，探查网速，检查特定网络状况等**

### 使用：

```
implementation 'com.hd:nethelper:1.0'
```

### 示例：

```
//添加网络状态监听(广播回调)
NetObserver.addObserver(this);

//检查网络是否连接
NetHelper.checkNetConnect(this);

//检查网络连接类型
NetHelper.getNetConnectTypeInfo(this);
NetHelper.getNetConnectType(this);
NetHelper.getNetConnectTypeStr(this);

//查询ip地址
NetHelper.getNetConnectAddress(this);

//检查指定ip是否可用
NetHelper.checkNetConnect("www.baidu.com");

//检查网速及质量
NetSpeedPassiveSampler sampler=new NetSpeedPassiveSampler(this,this);
sampler.startSampling();
sampler.stopSampling();

//其他使用方式可查看源码及demo
//...

```

### 内部依赖：

**[AndroidNetworkTools][0]：主要用作端口扫描，局域网设备查找，ping网络地址**

### 参考源码(感谢)：

**[network-connection-class][1]**

**[speedTestApp][2]**

### 在线网络测速工具：

**[中文版][3]**

**[英语版][4]**


[0]: https://github.com/stealthcopter/AndroidNetworkTools
[1]: https://github.com/facebook/network-connection-class
[2]: https://github.com/egcodes/speedTestApp
[3]: http://www.speedtest.cn/
[4]: http://www.speedtest.net/