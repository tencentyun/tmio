/**
 * Copyright (c) 2022 Tencent
 */

#pragma once

#include "tmio.h"

namespace tmio {

class TMIO_EXTERN TmioProxyListener {
 public:
    virtual ~TmioProxyListener() = default;

    virtual void onTmioConfig(Tmio *tmio) = 0;

    virtual void onStart(const char *local_addr, uint16_t local_port) = 0;

    enum ErrorType {
        UNKNOWN,
        // IO error on local tcp transport
        LOCAL_ERROR,
        // IO error on remote transport
        REMOTE_ERROR,
    };

    static const char *errorType(ErrorType type) {
        switch (type) {
            case LOCAL_ERROR:
                return "local";
            case REMOTE_ERROR:
                return "remote";
            default:
                return "unknown";
        }
    }

    virtual void onError(ErrorType type, const std::error_code &err) = 0;
};

/**
 * Bidirectional proxy
 *
 * Client <--(TCP)--> TmioProxy <--(Protocol)--> Remote Server
 */
class TMIO_EXTERN TmioProxy {
 public:
    static TmioProxy *create();

    static std::unique_ptr<TmioProxy> createUnique() {
        return std::unique_ptr<TmioProxy>(create());
    }

    TmioProxy() = default;
    virtual ~TmioProxy() = default;

    TmioProxy(const TmioProxy &) = delete;
    TmioProxy &operator==(const TmioProxy &) = delete;

    virtual void setListener(TmioProxyListener *listener) = 0;

    /**
     * start proxy
     *
     * @param local_url tcp://ip:port
     *                  Bind to random port if the port is not specified, and
     *                  return the port in onStart()
     * @param remote_url
     * 
     * @param config protocol dependent
     * @return
     */
    virtual std::error_code start(const std::string &local_url,
                                  const std::string &remote_url,
                                  void *config = nullptr) = 0;

    virtual std::error_code addRemoteLink(const std::string &remote_url,
                                  void *config = nullptr) = 0;

    virtual void stop() = 0;

    virtual std::error_code getStats(PerfStats *stats) = 0;
};

}  // namespace tmio