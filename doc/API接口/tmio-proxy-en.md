# TMIO Proxy Integration APIs

### Directions

*  ![](https://qcloudimg.tencent-cloud.cn/raw/0757428b829cae461a0849c594c3b304.png)

1. Create a `TmioProxy` instance.
2. Register a listener (`void setListener(TmioProxyListener *listener)`).
3. Start the instance (`std::error_code start(const std::string &local_url, const std::string &remote_url, void * config=nullptr)`).
4. After receiving the callback `onStart(const char *local_addr, uint16_t local_port)`, publish RTMP streams to the local address `rtmp://${local_addr}:${local_port}/${app}/${stream}?{query}`.
5. Stop publishing the streams.
6. Terminate the instance (TmioProxy `void stop()`) to release the resources.



__Notes__

* **If you use Tencent Cloud domains, add a `txHost=${server_domain}` field to the RTMP URL query string, such as**:

  `rtmp://127.0.0.1:8888/live/test?txSecret=***&txTime=***&txHost=livepush.myqcloud.com`

  **This is necessary because the RTMP server authenticates tcURLs. By default, URLs are converted to tcURLs (it's possible to generate tcURLs for both FFmpeg and libRTMP). For example, the above URL can be converted to the tcURL `rtmp://127.0.0.1:8888/live/test`. Because the tcURL does not match the server domain, authentication will fail. Configuring `txHost` can override the authentication of tcURLs.**

  For details, see [Tencent Cloud HTTPDNS Routing](https://cloud.tencent.com/document/product/267/36164).

* Each TMIO proxy corresponds to one publishing client. You cannot use one proxy with multiple publishing clients. Nor can you reuse a proxy (stop it and start it again).

* You can create multiple `TmioProxy` instances.

* `TmioProxy` does not have a timeout & retry mechanism. After it is started, `TmioProxy` will try to establish a connection with the remote server. If the remote server has a timeout mechanism, and the client fails to publish streams to `TmioProxy` within the timeout period, a timeout callback (`onError`) will be returned. In such cases, you can create another `TmioProxy` instance and publish the streams again.

  

------

### Creating a `TmioProxy` instance

```c++
    static TmioProxy *create();

    static std::unique_ptr<TmioProxy> createUnique();
```

----

### Registering a listener

```c++
void setListener(TmioProxyListener *listener);
```

Below are the callbacks of `TmioProxyListener`:

1. The TMIO configuration callback. You can configure TMIO parameters in this callback. **For simple configuration, you can use the method offered by `tmio-preset.h`**.

   ```c++
   void onTmioConfig(Tmio *tmio);
   ```

2. The callback for the start of `TmioProxy`.

   ```c++
   void onStart(const char *local_addr, uint16_t local_port);
   ```

   This callback indicates that the remote server is connected successfully, and the local TCP port is bound successfully. You can start publishing streams.

3. The error callback.

   ```c++
   void onError(ErrorType type, const std::error_code &err);
   ```

   You can use `ErrorType` to determine whether an error is a local or remote I/O error. A local I/O error is usually because RTMP streaming is stopped by the streamer. Therefore, if streaming has ended, you can ignore such errors. However, a remote I/O error usually needs to be handled.

-----------

### Starting the proxy

```c++
std::error_code start(const std::string &local_url, const std::string &remote_url, void * config=nullptr)
```

* Only the TCP scheme is supported for `local_url`, whose format is `tcp://${ip}:${port}`. `port` can be `0`, in which case a random port will be bound. The number of the successfully bound port will be returned to the application through the `onStart()` callback. Binding may fail if a specified port is occupied or if the server does not have permissions to the port. Setting `port` to `0` can avoid such issues.
* `remote_url` is the URL of the remote server.
* `config` is the configuration parameters. It is valid only if SRT connection bonding is enabled. For details, see [SRT Bonding Config](tmio-feature.md#srt-bonding-config-group).

----

### Stopping the proxy

```c++
void stop();
```
----