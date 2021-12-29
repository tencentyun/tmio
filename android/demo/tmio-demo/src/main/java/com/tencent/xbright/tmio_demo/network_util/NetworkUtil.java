package com.tencent.xbright.tmio_demo.network_util;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.LinkProperties;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import java.io.FileDescriptor;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

public class NetworkUtil {
    private static final String TAG = "NetworkUtil";

    private static Network cellular;

    public interface NetworkCallback {
        void onSuccess(Network network);

        void onFailure(Network network, String msg);
    }

    public static void requestCellular(Context context, @NonNull NetworkCallback callback) {
        Log.i(TAG, "request cellular network");
        ConnectivityManager connectivityManager =
                (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkRequest request = new NetworkRequest.Builder()
                .addTransportType(NetworkCapabilities.TRANSPORT_CELLULAR)
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build();
        NetworkCallbackImpl wrapper = new NetworkCallbackImpl(callback);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            connectivityManager.requestNetwork(request, wrapper, 2000);
        } else {
            connectivityManager.requestNetwork(request, wrapper);
        }
    }

    public static String getInterfaceAddr(boolean isWlan, boolean isIpv4) {
        try {
            List<NetworkInterface> interfaces = Collections.list(NetworkInterface.getNetworkInterfaces());
            for (NetworkInterface intf : interfaces) {
                if (isWlan) {
                    if (!intf.getName().contains("wlan0")) {
                        continue;
                    }
                } else {
                    if (intf.getName().contains("wlan0")) {
                        continue;
                    }
                }
                List<InetAddress> addrs = Collections.list(intf.getInetAddresses());
                for (InetAddress addr : addrs) {
                    if (!addr.isLoopbackAddress() && !addr.isLinkLocalAddress()) {
                        if ((isIpv4 && addr instanceof Inet4Address) ||
                                (!isIpv4 && addr instanceof Inet6Address)) {
                            return addr.getHostAddress();
                        }
                    }
                }
            }
        } catch (Exception ex) {
            Log.e(TAG, "getInterfaceAddr", ex);
        }
        return null;
    }

    public static ArrayList<String> getAllNetInterface() {
        ArrayList<String> allInterface = new ArrayList<>();

        try {
            Enumeration nis = NetworkInterface.getNetworkInterfaces();
            InetAddress ia = null;
            while (nis.hasMoreElements()) {
                NetworkInterface ni = (NetworkInterface) nis.nextElement();
                Enumeration<InetAddress> ias = ni.getInetAddresses();
                while (ias.hasMoreElements()) {
                    ia = ias.nextElement();
                    if (ia.isLoopbackAddress() || ia.isLinkLocalAddress()) {
                        continue;
                    }

                    String netinfo;
                    if (ia.getHostAddress().length() > 16) {
                        netinfo = ni.getName() + ":\n    " + ia.getHostAddress();
                    } else {
                        netinfo = ni.getName() + ": " + ia.getHostAddress();
                    }
                    allInterface.add(netinfo);
                    Log.i(TAG, "get all interface available interface:" + netinfo);
                }
            }
        } catch (SocketException e) {
            e.printStackTrace();
        }


        Log.i(TAG, "all interface:" + allInterface.toString());
        return allInterface;
    }

    public static boolean bindCellNetFd(FileDescriptor fd) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                if (cellular != null) {
                    cellular.bindSocket(fd);
                    return true;
                } else {
                    Log.i(TAG, "please apply for cellular network permission");
                }
            } catch(Exception e) {
                e.printStackTrace();
            }
        } else {
            Log.i(TAG, "current api level no support the function");
        }
        return false;
    }

    private static class NetworkCallbackImpl extends ConnectivityManager.NetworkCallback {
        private NetworkCallback callback;

        public NetworkCallbackImpl(@NonNull NetworkCallback callback) {
            this.callback = callback;
        }

        @Override
        public void onAvailable(@NonNull Network network) {
            super.onAvailable(network);
            Log.i(TAG, "onAvailable");
            cellular = network;
            callback.onSuccess(network);
        }

        @Override
        public void onLost(@NonNull Network network) {
            super.onLost(network);
            Log.i(TAG, "onLost.");
            cellular = null;
            callback.onFailure(network, "lost");
        }

        public void onUnavailable() {
            super.onUnavailable();
            Log.i(TAG, "onUnavailable.");
            cellular = null;
            callback.onFailure(null, "unavailable");
        }
    }
}
