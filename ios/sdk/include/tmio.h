#pragma once

#include <stdarg.h>
#include <stdint.h>

#include <functional>
#include <memory>
#include <string>
#include <vector>
#include <system_error>

#include "tmio-common.h"
#include "tmio-log.h"
#include "tmio-options.h"
#include "tmio-optmanager.h"
#include "tmio-stats.h"

namespace tmio {

static const int INVALID_SOCKET_VALUE = -1;

enum class Protocol {
    TCP,  // only for test, should not be used
    SRT,
    RIST,
};

enum class ControlCmd {
    // Tmio::control(ControlCmd, PerfStats *)
    GET_STATS = 1,
};

enum class NotifyType {
    NOTIFY_LINK_BROKEN = 1,
};

using NotifyCallback = std::function<void(NotifyType, void *)>;

enum class SrtTransMode {
    SRT_TRANS_DEFAULT = 0,
    SRT_TRANS_BROADCAST,
    SRT_TRANS_BACKUP,
    SRT_TRANS_BALANCING,
    SRT_TRANS_MULTICAST,
    SRT_TRANS_UNDEFINE,
};

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


    NetCardOption() : local_port(0),
                      socket_id(INVALID_SOCKET_VALUE) {}

    NetCardOption(const std::string &name, const std::string &src_addr,
                  const std::string &remote_url, int port, int weight,
                  int socket_id = INVALID_SOCKET_VALUE) {
        this->netcard = name;
        this->ipaddr = src_addr;
        this->remote_url = remote_url;
        this->local_port = port;
        this->weight = weight;
        this->socket_id = socket_id;
    }

};

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

    SrtFeatureConfig()
        : protocol(Protocol::SRT),
          trans_mode(SrtTransMode::SRT_TRANS_DEFAULT) {}

    SrtFeatureConfig(const SrtFeatureConfig &s) {
        protocol = s.protocol;
        trans_mode = s.trans_mode;
        callback = s.callback;
        vec_net_option = s.vec_net_option;
    }

    void addAvailableNet(const std::string &name, const std::string &ipaddr,
                         const std::string &remote_url, int port, int weight,
                         int socket_id) {
        vec_net_option.emplace_back(name, ipaddr, remote_url, port, weight, socket_id);
    }
};

class TMIO_EXTERN Tmio {
public:
    Tmio();
    virtual ~Tmio() = default;

    Tmio(const Tmio &) = delete;
    Tmio &operator=(const Tmio &) = delete;

    // For optname and value, ref. tmio-options.h
    bool setBoolOption(const std::string &optname, bool value) {
        return option_bool_.setOptionValue(optname, value);
    }

    bool setIntOption(const std::string &optname, int64_t value) {
        return option_int_.setOptionValue(optname, value);
    }

    bool setDoubleOption(const std::string &optname, double value) {
        return option_double_.setOptionValue(optname, value);
    }

    bool setStrOption(const std::string &optname, const std::string &value) {
        return option_str_.setOptionValue(optname, value);
    }

    bool getBoolOption(const std::string &optname, bool *value) {
        return option_bool_.getOptionValue(optname, value);
    }

    bool getIntOption(const std::string &optname, int64_t *value) {
        return option_int_.getOptionValue(optname, value);
    }

    bool getDoubleOption(const std::string &optname, double *value) {
        return option_double_.getOptionValue(optname, value);
    }

    bool getStrOption(const std::string &optname, std::string *value) {
        return option_str_.getOptionValue(optname, value);
    }

    std::string dumpOption();

    /**
     * open the stream specified by url
     *
     * @param config protocol dependent
     */
    virtual std::error_code open(const std::string &url,
                                 void *config = nullptr) = 0;

    /**
     * receive data
     *
     * @param err return error details
     * @return number of bytes which were received, or < 0 to indicate error
     */
    virtual int recv(uint8_t *buf, int len, std::error_code &err) = 0;

    /**
     * receive data callback
     *
     * Return true to continue the receiving loop, false to break the loop.
     * On error, len < 0 and err contains error details. The return value will
     * be ignored when error occured.
     */
    using RecvCallback = std::function<bool(const uint8_t *buf, int len,
                                            const std::error_code &err)>;
    /**
     * receive data in event loop
     *
     * recvLoop() block current thread, receive data in a loop and pass the data
     * to recv_cb
     */
    virtual void recvLoop(const RecvCallback &recv_cb) = 0;

    /**
     * send data
     *
     * @param err return error details
     * @return number of bytes which were sent, or < 0 to indicate error
     */
    virtual int send(const uint8_t *buf, int len, std::error_code &err) = 0;

    std::error_code control(ControlCmd cmd, ...);

    /**
     * interrupt open()/recv()/recvLoop()/send() from another thread
     */
    virtual void interrupt() = 0;

    virtual void close() = 0;

    virtual Protocol getProtocol() = 0;

protected:
    virtual std::error_code ctrl(ControlCmd cmd, va_list args) = 0;

    OptionManager<bool> option_bool_;
    OptionManager<int64_t> option_int_;
    OptionManager<double> option_double_;
    OptionManager<std::string> option_str_;

    bool thread_safe_check_ = false;
    int64_t timeout_ = -1;
    uint32_t recv_send_flags_ = base_options::FLAG_RECV_SEND;
};

class TMIO_EXTERN TmioFactory {
public:
    static Tmio *create(Protocol protocol);
    static std::string getSdkVersion();

    static std::unique_ptr<Tmio> createUnique(Protocol protocol) {
        return std::unique_ptr<Tmio>(create(protocol));
    }
};

}  // namespace tmio
