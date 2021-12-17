# Tmio API 接口说明 

------
##  Tmio 创建 (通过`TmioFactory `来创建)
```c++
class TMIO_EXTERN TmioFactory {
public:
    static Tmio *create(Protocol protocol);
    static std::string getSdkVersion();

    static std::unique_ptr<Tmio> createUnique(Protocol protocol) {
        return std::unique_ptr<Tmio>(create(protocol));
    }
};
```

### Protocol 参数
```c++
enum class Protocol {
    TCP,  // only for test, should not be used
    SRT,
    RIST,
};
```
------

## Tmio 参数配置接口
```c++
bool setBoolOption(const std::string &optname, bool value);

bool setIntOption(const std::string &optname, int64_t value);

bool setDoubleOption(const std::string &optname, double value);

bool setStrOption(const std::string &optname, const std::string &value);

bool getBoolOption(const std::string &optname, bool *value);

bool getIntOption(const std::string &optname, int64_t *value);

bool getDoubleOption(const std::string &optname, double *value);

bool getStrOption(const std::string &optname, std::string *value);
```
#### 参数名详见[tmio-option](tmio-option.md)

------

### 打开/连接

```c++
/**
 * open the stream specified by url
 *
 * @param config protocol dependent
 */
virtual std::error_code open(const std::string &url, void *config = nullptr) = 0;
```

- 根据具体协议，打开过程建立连接
- config接口，不同协议可自定义config对应的结构体和功能，例如，webrtc打开的过程需要信令通道，config参数可以作为信令通道传输配置接口，当前支持srt bonding 配置具体定义详见[SrtFeatureConfig](tmio-feature.md#SRT%20Bonding%20Config%20(Group))
- 建立连接是阻塞的过程，代码实现要考虑`open()`时用户调用`interrupt()`

------

### 下行接口


```c++
/**
 * receive data
 *
 * @param err return error details
 * @return number of bytes which were received, or < 0 to indicate error
 */
virtual int recv(uint8_t *buf, int len, std::error_code &err) = 0;

using RecvCallback = std::function<bool(const uint8_t *buf, int len, const std::error_code &err)>;
/**
 * receive data in event loop
 *
 * recvLoop() block current thread, receive data in a loop and pass the data to recvCallback
 * @param recvCallback return true to continue the receive loop, false for break
 */
virtual void recvLoop(const RecvCallback &recvCallback) = 0;
```
- 推流SDK，低优先级实现下行接口
- 设计两个拉流接口：`recv`和`recvLoop`
  - 根据协议和第三方库的实现，`recv`接口潜在用户调用不及时，内核或第三方库的buffer overflow的缺点。例如简单udp传输，如果不及时读出数据，数据在内核中被丢弃/覆盖。librist有类似问题：http://ffmpeg.org/pipermail/ffmpeg-devel/2021-February/276720.html
  - 对于这类场景，期望由协议库内部驱动，在循环中取数据，拿到数据后往用户设置的pipeline push数据，而不是由用户主动pull，所以添加`recvLoop`接口
  - `recvLoop`接口阻塞住用户线程，通过`RecvCallback` push数据给用户
  - 如果用户在`RecvCallback`中处理慢，仍然可能出现buffer overlow，无解。`recvLoop`只能在接口设计层面降低overlow的可能性，方便接入有类似设计的第三方库

----

### 上行

```c++
/**
 * send data
 *
 * @param err return error details
 * @return number of bytes which were sent, or < 0 to indicate error
 */
virtual int send(const uint8_t *buf, int len, std::error_code &err) = 0;
```

----

### 控制

```c++
std::error_code control(ControlCmd cmd, ...);
```

参数类型详见ControlCmd的注释。

----

### 中断
```C++
virtual void interrupt() = 0;
```

- `open()`, `recv()`, `recvLoop()`, `send()`接口是阻塞的，用户通过在其他线程调用`interrupt()`来中断阻塞的接口
- 难点：用户可能在`open()`之前、`open()`过程中、`close()`过程中、`close()`之后调用`interrupt()`，如何保证各个状态都能调用`interrupt()`程序状态无异常？设计上是否可优化？

----

### 关闭

```c++
virtual void close() = 0;
```

----

### robust, thread-safety

- `open()`, `recv()`, `recvLoop()`, `send()`, `close()`不保证线程安全，用户应当在同一个线程调用，或者用其他方式保证时序
- `interrupt()`应当在对象创建之后，`close()`之前调用，内部代码实现应当保证用户在`close()`前后调用`interrupt()`不产生副作用
- 如果用户没有调用`close()`，对象析构时应当处理没释放的资源，比如析构时再检查和调用`close()`

-----
