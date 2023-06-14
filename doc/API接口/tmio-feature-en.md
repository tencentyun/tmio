# Tmio Feature 

## Tmio Protocol
```c++
enum class Protocol {
    TCP,  // only for test, should not be used
    SRT,
    RIST,
};
```
* Currently, TMIO supports SRT and RIST for stream publishing. TCP is for test purposes.

---------------

## Tmio Control cmd
```c++
enum class ControlCmd {
    // Tmio::control(ControlCmd, PerfStats *)
    GET_STATS = 1,
};
```
* Currently, you can get the transfer status only if a single SRT connection is used.

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
* `SRT_TRANS_DEFAULT`: Do not use the connection bonding feature (only one connection will be used). This is the default setting.
* `SRT_TRANS_BROADCAST`: Use the broadcasting mode (connections are established according to the number of network interface cards configured). Each data packet is sent through all the connections.
* `SRT_TRANS_BACKUP`: Use the primary/backup mode (connections are established according to the number of network interface cards configured). One socket connection is used to send the data, while the other connections wait in the idle state, sending data regularly to stay alive. When the primary connection loses data packets or fails to respond, one of the idle connections will be activated.
* `SRT_TRANS_BALANCING`: Not supported yet.
* `SRT_TRANS_MULTICAST`: Not supported yet.
* `SRT_TRANS_UNDEFINE`: Undefined.

--------------

### SRT NetCardOption
```c++
struct NetCardOption{
    // Network card name, Used to specify the network card to send and receive
    // ifreq.ifr_name. eg:eth0/en0
    // Temporarily unused
    std::string netcard;

    // netcard ip address, used to bind udp client
    std::string ipaddr;

    // remote url. If the server has multiple network card addresses, this can be configured.
    std::string remote_url;

    // local udp client bind port
    int local_port;

    //Weight, priority range: 0-100.
    int weight;

    // udp socket id, if the value is not INVALID_SOCKET_VALUE, dup it to use
    // In order to adapt to the problem that the android system cannot use the data network after wifi is enabled,
    // the upper layer needs to do the bind operation
    int socket_id;
};
```
- `NetCardOption` structure

    * netcard - The name of the network interface card. When you receive a disconnection callback, this field tells you which card caused the problem.
    * ipaddr - The IP address of the network interface card bound. We recommend you use a real address instead of (0.0.0.0).
    * remote_url - The URL of the remote server.
    * local_port - The local port bound, which is randomly assigned by the system by default.
    * weight - The weight of the connection.
    * socket_id - The ID of the socket created by the upper layer application. The socket needs to be released by the upper layer application as well. The SDK will copy this socket.

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
- `SrtFeatureConfig` structure 
    * `protocol`: The protocol used. When `open` is called, this parameter is used to determine which protocol is used. For details, see [Tmio Protocol](#tmio-protocol).
    * `trans_mode`: If the SRT protocol is used, this parameter is used to determine whether connection bonding is enabled. For details, see [SRT Trans Mode](#srt-trans-mode).
    * `callback `: [NotifyCallback](#srt-bonding-config-group), which notifies you of a disconnection in the upper layer.
    * `vec_net_option`. The information of the teamed network interface cards. For details, see [SRT NetCardOption](#srt-netcardoption).