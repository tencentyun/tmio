#pragma once

#include <stdarg.h>

#include <chrono>
#include <functional>
#include <utility>

#include "tmio-common.h"

namespace tmio {

enum LogPriority {
    /** Verbose logging. Should typically be disabled for a release apk. */
    TMIO_LOG_VERBOSE = 1,
    /** Debug logging. Should typically be disabled for a release apk. */
    TMIO_LOG_DEBUG = 2,
    /** Informational logging. Should typically be disabled for a release apk.
     */
    TMIO_LOG_INFO = 3,
    /** Warning logging. For use with recoverable failures. */
    TMIO_LOG_WARN = 4,
    /** Error logging. For use with unrecoverable failures. */
    TMIO_LOG_ERROR = 5,
    /** Fatal logging. For use when aborting. */
    TMIO_LOG_FATAL = 6,
    /** No logging  */
    TMIO_LOG_SILENT = 7,
};

class TMIO_EXTERN Logger final {
public:
    static Logger &getInstance();

    using LogCallback =
        std::function<void(LogPriority, const char *, const char *)>;

    void setCallback(LogCallback func) { callback_ = std::move(func); }

    void setLevel(LogPriority level) { log_level_ = level; }

    LogPriority getLevel() { return log_level_; }

    /**
     * 是否同时从console输出
     *
     * 用于没设置callback的情况下做调试
     * 默认为true
     */
    void setConsoleOutput(bool enable) { console_output_ = enable; }

    void log(LogPriority level, const char *tag, const char *fmt, ...);

private:
    void output(LogPriority level, const char *tag, const char *msg);

private:
    LogPriority log_level_ = TMIO_LOG_WARN;
    bool console_output_ = true;
    LogCallback callback_;
    using Clock = std::chrono::high_resolution_clock;
    Clock::time_point start_time_ = Clock::now();
};

#ifndef LOG_TAG
#define LOG_TAG "tmio"
#endif

#define LOGD(fmt, ...)                                                  \
    tmio::Logger::getInstance().log(tmio::TMIO_LOG_DEBUG, LOG_TAG, fmt, \
                                    ##__VA_ARGS__)

#define LOGI(fmt, ...)                                                 \
    tmio::Logger::getInstance().log(tmio::TMIO_LOG_INFO, LOG_TAG, fmt, \
                                    ##__VA_ARGS__)

#define LOGW(fmt, ...)                                                 \
    tmio::Logger::getInstance().log(tmio::TMIO_LOG_WARN, LOG_TAG, fmt, \
                                    ##__VA_ARGS__)

#define LOGE(fmt, ...)                                                  \
    tmio::Logger::getInstance().log(tmio::TMIO_LOG_ERROR, LOG_TAG, fmt, \
                                    ##__VA_ARGS__)

// 输出函数名称
#define LOGFD(fmt, ...)                                                       \
    tmio::Logger::getInstance().log(tmio::TMIO_LOG_DEBUG, LOG_TAG, "%s " fmt, \
                                    __PRETTY_FUNCTION__, ##__VA_ARGS__)

#define LOGFI(fmt, ...)                                                      \
    tmio::Logger::getInstance().log(tmio::TMIO_LOG_INFO, LOG_TAG, "%s " fmt, \
                                    __PRETTY_FUNCTION__, ##__VA_ARGS__)
#define LOGFE(fmt, ...)                                                       \
    tmio::Logger::getInstance().log(tmio::TMIO_LOG_ERROR, LOG_TAG, "%s " fmt, \
                                    __PRETTY_FUNCTION__, ##__VA_ARGS__)

class TMIO_EXTERN LogInOut {
public:
    LogInOut(const char *str, LogPriority level = TMIO_LOG_INFO) : msg(str) {
        LOGI("%s >>>", msg);
    }

    ~LogInOut() { LOGI("%s <<<", msg); }

private:
    const char *msg;
};

#define LOGI_IN_OUT() tmio::LogInOut logInout(__PRETTY_FUNCTION__)

}  // namespace tmio
