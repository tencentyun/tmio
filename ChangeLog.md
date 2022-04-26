Release Version:

### 版本变更记录

#### v0.2.4-2-gdea22e5
  * 添加BBR开关，通过Option设置由上层应用选择是否启用bbr拥塞控制方式，示例

  ```C++
      // 设置拥塞控制为BBR方式
      tmio_->setStrOption(srt_options::CONGESTIONTYPE, srt_options::LIVE_BBR_RATESAMPLE);
  ```

  * bug 修复
    - 最后一个包或协商过程中丢包不重发问题修改
    - broadcast 弱网链路切换导致连接失败问题
    - bonding 模式发送错误异常处理修复（errcode=11）
    - 网络异常切换实时反馈优化，及时上报处理

  * tmio状态获取可根据 delivery_rate成员获取带宽情况
