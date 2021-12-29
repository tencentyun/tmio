package com.tencent.xbright.tmio_demo;

import android.util.Log;

import java.lang.annotation.Native;
import java.lang.ref.WeakReference;
import java.util.HashMap;

public class TmioProxy {
    private static final String TAG = "TmioProxy";

    private static final int EVENT_START = 0x01;
    private static final int EVENT_ERROR = 0x02;
    private static final int EVENT_NOTIFY = 0x03;


    public static final int ERROR_TYPE_UNKNOWN = 0;
    public static final int ERROR_TYPE_LOCAL_ERROR = 1;
    public static final int ERROR_TYPE_REMOTE_ERROR = 2;

    /* These values correspond to SrtTransMode in tmio.h，
     * do not change these values without updating their counterparts
     */
    public static final int TMIO_BONDING_MODE_DEFAULT = 0;
    public static final int TMIO_BONDING_MODE_BROADCAST = 1;
    public static final int TMIO_BONDING_MODE_BACKUP = 2;


    private static final HashMap<Long, WeakReference<TmioProxy>> ptrToObjectMap = new HashMap<>();

    private long ptr;

    private TmioProxyListener listener;

    public interface TmioProxyListener {
        void onStart(String host, int port);

        void onError(int errType, int errCode, String errMsg);

        void onNotify(String notifyMsg);
    }

    public TmioProxy() {
        ptr = create();
        if (ptr == 0) {
            throw new RuntimeException("create TmioProxy failed");
        }
        ptrToObjectMap.put(ptr, new WeakReference<>(this));
    }

    public void release() {
        Log.i(TAG, "release");
        if (ptr == 0) {
            return;
        }
        release(ptr);
        ptrToObjectMap.remove(ptr);
        ptr = 0;
    }
    public int createSocket(String localAddr) {
        return createBondingSocket(ptr, localAddr);
    }
    public void setListener(TmioProxyListener listener) {
        this.listener = listener;
    }

    public void start(String localUrl, String remoteUrl, int packageLen, int mode) {
        start(ptr, localUrl, remoteUrl, packageLen, mode);
    }

    public void stop() {
        Log.i(TAG, "stop");
        stop(ptr);
    }

    public void setFeatureConfig(String interface_name, String ipaddr, String remote_url,
                                 int port, int weight, int socket_id) {
        addFeatureConfig(ptr, interface_name, ipaddr, remote_url, port, weight, socket_id);
    }

    public void addnewLink(String interface_name, String ipaddr, String remote_url,
                           int port, int weight, int socket_id) {
        addNewLinkConfig(ptr, interface_name, ipaddr, remote_url, port, weight, socket_id);
    }

    public String getStats() {
        return getStats(ptr);
    }

    private static void onEvent(long ptr, int event, long arg1, long arg2, String str) {
        WeakReference<TmioProxy> instance = ptrToObjectMap.get(ptr);
        if (instance == null) {
            return;
        }
        TmioProxy proxy = instance.get();
        if (proxy == null) {
            return;
        }
        switch (event) {
            case EVENT_START:
                proxy.onStart(str, (int) arg1);
                break;
            case EVENT_ERROR:
                proxy.onError((int)arg1, (int)arg2, str);
                break;
            case EVENT_NOTIFY:
                proxy.onNotify(str);
                break;
            default:
                Log.e(TAG, "unknown event " + event);
                break;
        }
    }

    private void onStart(String host, int port) {
        TmioProxyListener tmp = listener;
        if (tmp != null) {
            tmp.onStart(host, port);
        }
    }

    private void onError(int errType, int errCode, String errMsg) {
        TmioProxyListener tmp = listener;
        if (tmp != null) {
            tmp.onError(errType, errCode, errMsg);
        }
    }

    private void onNotify(String notifyMsg) {
        TmioProxyListener tmp = listener;
        if (tmp != null) {
            tmp.onNotify(notifyMsg);
        }
    }


    private static native long create();
    private static native void release(long ptr);
    private static native void start(long ptr, String localUrl, String remoteUrl,
                                     int packageLenFixed, int mode);
    private static native void stop(long ptr);
    private static native String getStats(long ptr);
    private static native int createBondingSocket(long ptr, String localAddr);

    private static native void addFeatureConfig(long ptr, String interface_name, String ipaddr,
                                                String remote_url, int port, int weight, int socket_id);
    private static native void addNewLinkConfig(long ptr, String interface_name, String ipaddr,
                                                String remote_url, int port, int weight, int socket_id);

}