#include <jni.h>
#include <thread>
#include <sys/socket.h>
#include <netinet/in.h>
#include <arpa/inet.h>
#include <unistd.h>
#include <netdb.h>

#include "tmio.h"
#include "tmio-preset.h"
#include "tmio-proxy.h"

#define com_tencent_xbright_tmio_demo_TmioProxy_EVENT_START 1

#define com_tencent_xbright_tmio_demo_TmioProxy_EVENT_ERROR 2

#define com_tencent_xbright_tmio_demo_TmioProxy_EVENT_NOTIFY 3

static JavaVM * gJVM = nullptr;


class AutoDetach {
public:
    ~AutoDetach() {
        if (enable_) {
            gJVM->DetachCurrentThread();
        }
    }

    void enable() {
        enable_ = true;
    }

private:
    bool enable_ = false;
};

static JNIEnv *getEnv(AutoDetach &auto_detach) {
    JNIEnv *env = nullptr;
    jint ret = gJVM->GetEnv((void **) &env, JNI_VERSION_1_6);
    if (ret == JNI_EDETACHED) {
        gJVM->AttachCurrentThread(&env, nullptr);
        auto_detach.enable();
    }
    return env;
}


class TmioProxyJni : public tmio::TmioProxyListener {
public:
    explicit TmioProxyJni(JNIEnv *env, jclass clazz)  {
        tmio::Logger::getInstance().setLevel(tmio::LogPriority::TMIO_LOG_VERBOSE);
        proxy_ = tmio::TmioProxy::createUnique();
        clazz_ = (jclass)env->NewGlobalRef(clazz);
        on_event_method_ = env->GetStaticMethodID(clazz, "onEvent",
                                                  "(JIJJLjava/lang/String;)V");
        proxy_->setListener(this);
        option_.protocol = tmio::Protocol::SRT;
    }

    ~TmioProxyJni() override {
        proxy_->stop();
        proxy_ = nullptr;

        AutoDetach auto_detach;
        getEnv(auto_detach)->DeleteGlobalRef(clazz_);
    }

    int createBondingSocket(JNIEnv *env, jstring localAddr) {
        auto local_addr = env->GetStringUTFChars(localAddr, nullptr);

        struct addrinfo hints, *res;
        bzero(&hints, sizeof(hints));
        hints.ai_flags = AI_PASSIVE;
        hints.ai_family = AF_UNSPEC;
        hints.ai_protocol = IPPROTO_IP;
        int s = 0;

        if (0 != (s=getaddrinfo(local_addr, nullptr, &hints, &res))) {
            LOGFE("getaddrinfo err, local_addr:%s, err:%s", local_addr, gai_strerror(s));
            return -1;
        }

        union {
            sockaddr_storage storage;
            sockaddr_in in;
            sockaddr_in6 in6;
        } sock_union_local;

        int socket_id = socket(res->ai_family, SOCK_DGRAM, IPPROTO_UDP);
        bzero(&sock_union_local, sizeof(sock_union_local));

        sock_union_local.storage.ss_family = res->ai_family;

        if (res->ai_family == AF_INET) {
            sock_union_local.in.sin_port = htons(0);
            inet_pton(AF_INET, local_addr, &sock_union_local.in.sin_addr);
        } else if (res->ai_family == AF_INET6) {
            sock_union_local.in6.sin6_port = htons(0);
            inet_pton(AF_INET6, local_addr, &sock_union_local.in6.sin6_addr);
        } else {
            LOGFE("no support the ip protocol family");
        }

        if (bind(socket_id, (struct sockaddr *)&sock_union_local, sizeof(sock_union_local)) < 0) {
            LOGFE("bind error:%s", local_addr);
        }

        if (!res) {
            freeaddrinfo(res);
            res = nullptr;
        }
        return socket_id;
    }

    void notifyCallBack(tmio::NotifyType type, void *info) {
        if (type == tmio::NotifyType::NOTIFY_LINK_BROKEN) {
            notify_link_broken(info);
        } else {
            LOGFE("unknown type %d", static_cast<int>(type));
        }
    }

    void addNewLink(JNIEnv *env, jstring interface_name, jstring ipaddr,
                    jstring remote, jint port, jint weight, jint socket_id) {
        auto name = env->GetStringUTFChars(interface_name, nullptr);
        auto addr = env->GetStringUTFChars(ipaddr, nullptr);
        auto re_url = env->GetStringUTFChars(remote, nullptr);

        tmio::SrtFeatureConfig newLinkOption_;
        newLinkOption_.protocol = tmio::Protocol::SRT;
        newLinkOption_.callback = std::bind(&TmioProxyJni::notifyCallBack, this,
                                            std::placeholders::_1, std::placeholders::_2);
        newLinkOption_.trans_mode = option_.trans_mode ;
        newLinkOption_.addAvailableNet(name, addr, re_url, port, weight, socket_id);
        proxy_->addRemoteLink(re_url, &newLinkOption_);
    }

    void addFeatureConfig(JNIEnv *env, jstring interface_name, jstring ipaddr,
                          jstring remote, jint port, jint weight, jint socket_id) {
        auto name = env->GetStringUTFChars(interface_name, nullptr);
        auto addr = env->GetStringUTFChars(ipaddr, nullptr);
        auto re_url = env->GetStringUTFChars(remote, nullptr);
        option_.addAvailableNet(name, addr, re_url, port, weight, socket_id);
    }


    void start(JNIEnv *env, jstring local, jstring remote, jint packageLenFixed, jint mode) {
        auto localUrl = env->GetStringUTFChars(local, nullptr);
        auto remoteUrl = env->GetStringUTFChars(remote, nullptr);
        option_.trans_mode = (tmio::SrtTransMode)mode;
        option_.callback = std::bind(&TmioProxyJni::notifyCallBack, this,
                                     std::placeholders::_1, std::placeholders::_2);
        auto err = proxy_->start(localUrl, remoteUrl, mode == 0 ? nullptr : &option_);
        env->ReleaseStringUTFChars(local, localUrl);
        env->ReleaseStringUTFChars(remote, remoteUrl);
        if (err) {
            onError(ErrorType::UNKNOWN, err);
        }
    }

    void stop() {
        proxy_->stop();
    }

    jstring getStats(JNIEnv *env) {
        tmio::PerfStats stats;
        proxy_->getStats(&stats);
        auto str = stats.toStr(true);
        return env->NewStringUTF(str.c_str());
    }

private:
    void onTmioConfig(tmio::Tmio *tmio) override {
        tmio::SrtPreset::rtmp(tmio, true);
    }

    void onStart(const char *local_addr, uint16_t local_port) override {
        AutoDetach auto_detach;
        auto env = getEnv(auto_detach);
        auto jstr = env->NewStringUTF(local_addr);
        env->CallStaticVoidMethod(clazz_, on_event_method_,
                                  (jlong)this,
                                  com_tencent_xbright_tmio_demo_TmioProxy_EVENT_START,
                                  (jlong) local_port,
                                  (jlong)0L,
                                  jstr);

        gJVM->DetachCurrentThread();
    }

    void onError(ErrorType type, const std::error_code &err) override {
        AutoDetach auto_detach;
        auto env = getEnv(auto_detach);
        auto jstr = env->NewStringUTF(err.message().c_str());
        env->CallStaticVoidMethod(clazz_, on_event_method_,
                                  (jlong)this,
                                  com_tencent_xbright_tmio_demo_TmioProxy_EVENT_ERROR,
                                  (jlong)type,
                                  (jlong)err.value(),
                                  jstr);
    }

    void notify_link_broken(void * configParam) {
        AutoDetach auto_detach;
        auto env = getEnv(auto_detach);
        tmio::NetCardOption *option = reinterpret_cast<tmio::NetCardOption *>(configParam);
        LOGFE("err link:[src=%s, dst=%s]", option->ipaddr.c_str(),
              option->remote_url.c_str());
        auto jstr = env->NewStringUTF(option->ipaddr.c_str());
        env->CallStaticVoidMethod(clazz_, on_event_method_,
                                  reinterpret_cast<jlong>(this),
                                  com_tencent_xbright_tmio_demo_TmioProxy_EVENT_NOTIFY,
                                  static_cast<jlong>(0L),
                                  static_cast<jlong>(0L),
                                  jstr);
    }

private:
    std::unique_ptr<tmio::TmioProxy> proxy_;
    tmio::SrtFeatureConfig option_;

    jclass clazz_;
    jmethodID on_event_method_;
};

/*
 * Class:     com_tencent_xbright_tmio_demo_TmioProxy
 * Method:    create
 * Signature: ()J
 */
extern "C"
JNIEXPORT jlong JNICALL
Java_com_tencent_xbright_tmio_1demo_TmioProxy_create(JNIEnv *env,
                                                     jclass clazz) {
    auto proxy = new TmioProxyJni(env, clazz);
    return (jlong)proxy;
}

/*
 * Class:     com_tencent_xbright_tmio_demo_TmioProxy
 * Method:    release
 * Signature: (J)V
 */
extern "C"
JNIEXPORT void JNICALL
Java_com_tencent_xbright_tmio_1demo_TmioProxy_release(JNIEnv *,
                                                      jclass,
                                                      jlong ptr) {
    auto proxy = (TmioProxyJni*)ptr;
    delete proxy;
}

/*
 * Class:     com_tencent_xbright_tmio_demo_TmioProxy
 * Method:    start
 * Signature: (JLjava/lang/String;Ljava/lang/String;)V
 */
extern "C"
JNIEXPORT void JNICALL
Java_com_tencent_xbright_tmio_1demo_TmioProxy_start(JNIEnv *env,
                                                    jclass,
                                                    jlong ptr,
                                                    jstring local,
                                                    jstring remote,
                                                    jint packageLenFixed,
                                                    jint mode) {
    auto proxy = (TmioProxyJni *) ptr;
    if (proxy == nullptr)
        return;
    proxy->start(env, local, remote, packageLenFixed, mode);
}

/*
 * Class:     com_tencent_xbright_tmio_demo_TmioProxy
 * Method:    stop
 * Signature: (J)V
 */
extern "C"
JNIEXPORT void JNICALL
Java_com_tencent_xbright_tmio_1demo_TmioProxy_stop(JNIEnv *,
                                                                                     jclass,
                                                                                     jlong ptr) {
    auto proxy = (TmioProxyJni *) ptr;
    if (proxy == nullptr)
        return;
    proxy->stop();
}

extern "C"
JNIEXPORT void JNICALL
Java_com_tencent_xbright_tmio_1demo_TmioProxy_addFeatureConfig(JNIEnv *env,
                                                               jclass clazz,
                                                               jlong ptr,
                                                               jstring interface_name,
                                                               jstring ipaddr,
                                                               jstring remote_url,
                                                               jint port,
                                                               jint weight,
                                                               jint socket_id) {
    // TODO: implement addFeatureConfig()
    auto proxy = (TmioProxyJni *) ptr;
    if (proxy == nullptr)
        return;
    proxy->addFeatureConfig(env, interface_name, ipaddr, remote_url, port, weight, socket_id);
}

extern "C"
JNIEXPORT void JNICALL
Java_com_tencent_xbright_tmio_1demo_TmioProxy_addNewLinkConfig(JNIEnv *env,
                                                               jclass clazz,
                                                               jlong ptr,
                                                               jstring interface_name,
                                                               jstring ipaddr,
                                                               jstring remote_url,
                                                               jint port,
                                                               jint weight,
                                                               jint socket_id) {
    // TODO: implement addNewLinkConfig()
    auto proxy = (TmioProxyJni *) ptr;
    if (proxy == nullptr)
        return;
    proxy->addNewLink(env, interface_name, ipaddr, remote_url, port, weight, socket_id);
}

extern "C"
JNIEXPORT jstring JNICALL
Java_com_tencent_xbright_tmio_1demo_TmioProxy_getStats(JNIEnv *env,
                                                       jclass,
                                                       jlong ptr) {
    auto proxy = (TmioProxyJni *) ptr;
    if (proxy == nullptr)
        return nullptr;
    return proxy->getStats(env);
}

extern "C"
JNIEXPORT jint JNICALL
Java_com_tencent_xbright_tmio_1demo_TmioProxy_createBondingSocket(JNIEnv *env,
                                                                  jclass clazz,
                                                                  jlong ptr,
                                                                  jstring localAddr) {
    // TODO: implement createBondingSocket()
    auto proxy = (TmioProxyJni *) ptr;
    if (proxy == nullptr)
        return -1;
    return proxy->createBondingSocket(env, localAddr);
}

extern "C" JNIEXPORT jint JNI_OnLoad(JavaVM *vm, void *reserved) {
    gJVM = vm;
//    TmioProxyJni::setJavaVM(vm);
    return JNI_VERSION_1_6;
}

extern "C" JNIEXPORT void JNI_OnUnload(JavaVM *vm, void *reserved) {
//    TmioProxyJni::setJavaVM(nullptr);
    gJVM = nullptr;
}


extern "C"
JNIEXPORT jstring JNICALL
Java_com_tencent_xbright_tmio_1demo_TmioDemo_getSdkVersion(JNIEnv *env,
                                                           jclass clazz) {
    // TODO: implement getSdkVersion()
    std::string sdkVersion = tmio::TmioFactory::getSdkVersion();
    return env->NewStringUTF(sdkVersion.c_str());
}