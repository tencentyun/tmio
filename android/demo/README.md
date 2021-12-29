## 使用说明
  - 此demo 主要验证tmio sdk 基本功能，其主要功能为通过代理的方式将rtmp数据以UDT协议上传推流，
  主要分为两种模式单链路推流（default）和bonding多网卡推流（分为broadcast和backup）。
  	* 单链路推流即使用默认网卡实现的单路推流模式，在android设备上如果已连接wifi则使用wlan，
	  未连接wifi则需要启动数据网络，通过数据流量来实现上传推流。
  	* Bonding 多网卡推流则是使用多网卡建立多路链接推流，在android demo中需要连接wifi 同时启动数据网络才可使用，
  	由于android 设备在连接wifi后，数据网络无法直接使用，需要申请4G/5G的数据网络权限，所以若要测试数据bonding 多网卡，
	需要点击申请4G/5G权限（在启动后点击一次即可，不需要每次都申请）。
  	* 多网卡broadcast 模式表示多链路同时推流，backup模式则表示默认使用一条链路推流，当此链路不稳定时则启动备用链路。

### Tmio Test 页面
  - 获取当前可用网卡信息-可查看当前可用的网卡信息
  - 申请4G/5G网卡测试-当连接wifi时，测试多网卡问题则需要申请
  - 查询sdk 版本信息-获取SDK版本信息，用来确认sdk版本是否正确对应tmio的测试tag 版本信息


### Tmio Proxy 页面
  - 本地URL-本地代理地址端口，用来接收数据
  - 协议-ts/rtmp，当前主要测试rtmp协议，ts协议暂不支持
  - 远程URL-服务侧远端地址
  - 播放URL-可边推流边播放的URL地址，仅支持hls m3u8格式地址

  - Default-默认单链路推流
  - Broadcast-多网卡广播推流
  - Backup-多网卡备份（当主链路不稳定时则激活备用链路）

  - 开启代理-启动代理建立与远端服务的连接，同时启动本地监听
  - 开始推流-调用第三方ffmpeg 实现rtmp 推流（也可以使用其他工具来实现推流）

  - +WIFI链接-可在测试wifi网络断开又重连后，再次添加一条wifi链路（需手动添加）
  - +数据链接-当数据网络断开重新连接后，再次添加一条数据链路（需手动添加）

  - 开始播放-开始请求播放


### 其他说明

1.	配置保存，本地URL/远端URL/播放URL可手动修改再使用此修改后会保存至配置文件，下次启动可直接读取
2.	在测试bonding 多链路时，若有链路断开会有提示链路IP地址断开通知
3.	播放为HLS 请求，延时较高， 可使用vlc/ffplay等播放工具使用rtmp来拉流播放
4.	播放URL说明：


### 示例

```
PC端FFmpeg推流 ====> Android Tmio Proxy转推 ====> 腾讯云SRT服务器
PC端FFmpeg推流 ====> Android Tmio Proxy转推 ====> FFmpeg/FFplay SRT
```

腾讯云SRT服务器支持TS over SRT和RTMP over SRT，FFmpeg/FFplay做SRT server支持TS over SRT，不支持RTMP over SRT。

基本流程：
1. 启动Android Tmio Proxy应用
2. 配置本地URL `tcp://${android_ip}:${android_port}`，远程URL `srt://${ip}:${port}`。如果使用腾讯云SRT服务器，远程URL需要带上streamid `srt://${ip}:${port}?streamid=${streamid}`
3. 如果远程URL为FFmpeg/FFplay，启动FFmpeg/FFplay
4. 点击Android Tmio Proxy ”开始代理“ 按钮
5. PC端启动推流

### FFmpeg/FFplay做SRT server

```sh
ffmpeg -mode listener -i "srt://${pc_ip}:${pc_port}" -c copy -f mpegts test.ts
```
或
```sh
ffplay -mode listener "srt://${pc_ip}:${pc_port}"
```

### PC推流到Android设备

推流到腾讯云SRT服务器，RTMP over SRT模式：
```sh
ffmpeg -re -i test.mp4 -c copy -f flv "rtmp://${android_ip}:${android_port}/${app}/${stream}?txSecret=${txSecret}&txTime=${txTime}&txHost=${srt_server_domain}"
```

推流到FFmpeg，TS over SRT模式：
```sh
ffmpeg -re -i test.mp4 -c copy -f mpegts "tcp://${android_ip}:${android_port}"
```