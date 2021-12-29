# Tmio Feature 

## Tmio Protocol
```c++
enum class Protocol {
    TCP,  // only for test, should not be used
    SRT,
    RIST,
};
```
* tmio 当前支持SRT 和RIST两种协议实现推流，TCP只做为测试使用

---------------

## Tmio Control cmd
```c++
enum class ControlCmd {
    // Tmio::control(ControlCmd, PerfStats *)
    GET_STATS = 1,
};
```
* 当前仅支持SRT 单链路下的获取网络传输状态

--------------
## SRT Feature

### SRT Trans Mode
```c++
enum class SrtTransMode {
    SRT_TRANS_DEFAULT = 0,
    SRT_TRANS_BROADCAST,
    SRT_TRANS_BACKUP,
    SRT_TRANS_BALANCING,
    SRT_TRANS_MULTICAST,
    SRT_TRANS_UNDEFINE,
};
```
* `SRT_TRANS_DEFAULT`： 默认不使用bonding功能(单链路传输)
* `SRT_TRANS_BROADCAST`： 广播模式(根据配置的网卡数量建立连接), 每一个数据包都通过每一条链接发送一份
* `SRT_TRANS_BACKUP`：主备模式(根据配置的网卡数量建立连接), 使用一条socket链接发送数据，其他链接作为备用一直处于idle状态定时发送keeplive，当active链路出现丢包或长时间无响应等异常时，从idle状态集中激活一条链路使用
* `SRT_TRANS_BALANCING`： 暂不支持
* `SRT_TRANS_MULTICAST`： 暂不支持
* `SRT_TRANS_UNDEFINE`： 未定义

--------------

### SRT NetCardOption
```c++
struct NetCardOption{
    // Network card name, Used to specify the network card to send and receive
    // ifreq.ifr_name. eg:eth0/en0
    // Temporarily unused
    std::string netcard;

    // netcard ip address，used to bind udp client
    std::string ipaddr;

    // remote url，If the server has multiple network card addresses, this can be configured
    std::string remote_url;

    // local udp client bind port
    int local_port;

    //Weight, priority range【0-100】
    int weight;

    // udp socket id, if the value is not INVALID_SOCKET_VALUE, dup it to use
    // In order to adapt to the problem that the android system cannot use the data network after wifi is enabled,
    // the upper layer needs to do the bind operation
    int socket_id;
};
```
- NetCardOption 结构解析

    * netcard - 网卡名称，当前主要为回调通知链路异常断开时方便用户知道哪个网卡异常
    * ipaddr - 链路传输绑定的网卡地址，建议使用真实地址，不要使用（0.0.0.0）
    * remote_url - 远端服务侧地址
    * local_port - 本地绑定端口，默认系统随机分配
    * weight - 链路权重
    * socket_id - 上层应用创建的socket id， sdk会复制它，需上层应用自己释放

--------------

### SRT Bonding Config (Group)

```c++
enum class NotifyType {
    NOTIFY_LINK_BROKEN = 1,
};

using NotifyCallback = std::function<void(NotifyType, void *)>;

struct TMIO_EXTERN SrtFeatureConfig {
    // The proxy judges which protocol to use according to the protocol,
    // and the first member variable of the new config must be protocol.
    Protocol protocol;
    SrtTransMode trans_mode;

    // NotifyType type        |    void *info
    // -----------------------------------------------------------
    // NOTIFY_LINK_BROKEN     |    pointer to NetCardOption
    NotifyCallback callback;

    std::vector<tmio::NetCardOption> vec_net_option;
};

```
- `SrtFeatureConfig`结构 
    * `protocol`： 定义了使用何种协议，在open调用时，需根据此参数判断是使用哪种协议，以便保存，具体协议详见 [Tmio Protocol](#tmio-protocol)
    * `trans_mode`： 为srt传输模式，据此来判断是否启用bonging功能, 详见[SRT Trans Mode](#srt-trans-mode)
    * `callback ` ：[NotifyCallback](#srt-bonding-config-group)回调通知接口，通知上层有链路断开
    * `vec_net_option`： 组内使用网卡成员信息，详见[SRT NetCardOption](#srt-netcardoption)
