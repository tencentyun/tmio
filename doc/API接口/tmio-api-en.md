# TMIO APIs 

------
## Create a `Tmio` instance** (using `TmioFactory`)
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

### Protocol
```c++
enum class Protocol {
    TCP,  // For tests
    SRT,
    RIST, // Supported
};
```
------

## TMIO configuration APIs
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
#### For details about the parameters, see [tmio-option](tmio-option.md).

------

### Start/Connect

```c++
/**
 * open the stream specified by url
 *
 * @param config protocol dependent
 */
virtual std::error_code open(const std::string &url, void *config = nullptr) = 0;
```

- Start the stream and establish a connection according to the protocol specified.
- You can define the data structure and of `config` according to the protocol used. For example, for WebRTC, because a signaling channel is required when the stream is started, you can use `config` to configure the signaling channel. SRT connection bonding is supported. For details, see [SrtFeatureConfig](tmio-feature.md#srt-bonding-config-group).
- The thread will be blocked when a connection is being established. Therefore, after `open()`, make sure you call `interrupt()`.

------

### Receive data


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
- TMIO is primarily used to optimize publishing. Playback APIs have a lower priority.
- Two playback APIs are offered: `recv` and `recvLoop`.
  - With `recv`, depending on the protocol and third-party library used, there is a potential buffer overflow issue in the kernel or in the third-party library caused by failure to call the API in time. For example, for simple UDP transfer, the data in the kernel may be lost or overwritten if it is not obtained in time. libRIST has the same problem (http://ffmpeg.org/pipermail/ffmpeg-devel/2021-February/276720.html).
  - One solution to that issue is to have the protocol library pull data from a loop and then push the data to the pipeline you specify instead of waiting for the user to pull the data. That's why the `recvLoop` API is designed.
  - `recvLoop` blocks your thread and returns the data via `RecvCallback`.
  - However, if processing in `RecvCallback` takes a long time, it may still cause the buffer overflow issue. `recvLoop` only reduces the probability of the issue occurring and makes it easier to use a third-party library with a similar design.

----

### Publish data

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

### Control

```c++
std::error_code control(ControlCmd cmd, ...);
```

For details about the parameters, see the comment for `ControlCmd`.

----

### Interrupt
```c++
virtual void interrupt() = 0;
```

- The `open()`, `recv()`, `recvLoop()`, and `send()` APIs block the thread. You can call `interrupt()` in another thread to stop the APIs.
- A challenge here is how to ensure that `interrupt()` works in every stage (it may be called before or during the execution of `open()`, as well as during or after the execution of `close()`).

----

### Terminate

```c++
virtual void close() = 0;
```

----

### Robustness and thread safety

- `open()`, `recv()`, `recvLoop()`, `send()`, and `close()` do not guarantee thread safety. Make sure you call them in the same thread so that they are executed in the correct order.
- `interrupt()` should be called after an object is created and before `close()` is called. Also, make sure there will be no unintended consequences if you call `interrupt()` before or after `close()`.
- If you didn't call `close()`, make sure you release the resources (call `close()`) when destructing the object.

-----