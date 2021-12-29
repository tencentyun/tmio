package com.tencent.xbright.tmio_demo.ui.proxy;

import android.app.Application;
import android.net.Network;
import android.os.ParcelFileDescriptor;
import android.util.Log;

import androidx.annotation.UiThread;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.tencent.xbright.tmio_demo.TmioProxy;
import com.tencent.xbright.tmio_demo.TmioProxy.TmioProxyListener;
import com.tencent.xbright.tmio_demo.network_util.NetworkUtil;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Locale;

public class TmioProxyViewModel extends AndroidViewModel {
    private TmioProxy tmioProxy;
    private static final String TAG = "TmioProxyViewModel";

    private final MutableLiveData<String> localAddr;
    private final MutableLiveData<String> errorInfo;
    private final MutableLiveData<String> notifyInfo;

    private ParcelFileDescriptor cellularFileDescriptor = null;

    public TmioProxyViewModel(Application application) {
        super(application);
        localAddr = new MutableLiveData<>();
        errorInfo = new MutableLiveData<>();
        notifyInfo = new MutableLiveData<>();
    }

    @UiThread
    public void start(String localUrl, String remoteUrl, String protocol, int mode) {
        if (tmioProxy != null) {
            tmioProxy.stop();
            tmioProxy.release();
        }
        tmioProxy = new TmioProxy();
        if (mode != 0) {
            String localAddr = NetworkUtil.getInterfaceAddr(true, true);

            if (localAddr != null) {
                tmioProxy.setFeatureConfig("wlan", localAddr, remoteUrl, 0, 1, -1);
            }

            String cellIpaddr = NetworkUtil.getInterfaceAddr(false, true);
            if (cellIpaddr != null) {
                Log.d(TAG, "cellIP:" + cellIpaddr);
                int sock_id = tmioProxy.createSocket(cellIpaddr);
                try {
                    cellularFileDescriptor = ParcelFileDescriptor.adoptFd(sock_id);
                    if (NetworkUtil.bindCellNetFd(cellularFileDescriptor.getFileDescriptor())) {
                        tmioProxy.setFeatureConfig("cell", cellIpaddr, remoteUrl, 0, 0, sock_id);
                    } else {
                        notifyInfo.postValue("绑定移动网络失败，当前系统版本不支持此功能，建议升级");
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            } else {
                notifyInfo.postValue("无法获取数据网络地址，请确认是否已开启数据流量上网。");
            }
        }

        int packageLen = -1;
        if (protocol.equals("TS")) {
            packageLen = 188;
        }

        tmioProxy.setListener(proxyListener);
        tmioProxy.start(localUrl, remoteUrl, packageLen, mode);
    }

    @UiThread
    public void addWifiLink(String remoteUrl) {
        if (tmioProxy == null) {
            return ;
        }

        String localWifiaddr = NetworkUtil.getInterfaceAddr(true, true);
        if (localAddr != null) {
            tmioProxy.addnewLink("wlan", localWifiaddr, remoteUrl, 0, 1, -1);
        }
    }

    @UiThread
    public void addCellNetLink(String remoteUrl) {
        if (tmioProxy == null) {
            return;
        }

        String localCelladdr = NetworkUtil.getInterfaceAddr(false, true);
        if (localCelladdr == null) {
            notifyInfo.postValue("无法获取数据网络地址，请确认是否已开启数据流量上网。");
            return ;
        }
        int sock_id = tmioProxy.createSocket(localCelladdr);
        try {
            final ParcelFileDescriptor parcelFileDescriptor = ParcelFileDescriptor.adoptFd(sock_id);
            if (NetworkUtil.bindCellNetFd(parcelFileDescriptor.getFileDescriptor())) {
                tmioProxy.addnewLink("cell", localCelladdr, remoteUrl, 0, 0, sock_id);
            } else {
                notifyInfo.postValue("绑定移动网络失败，当前系统版本不支持此功能，建议升级");
            }
            parcelFileDescriptor.close();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    @UiThread
    public void stop() {
        if (tmioProxy != null) {
            tmioProxy.stop();
            tmioProxy.release();
        }
    }


    public LiveData<String> getLocalAddr() {
        return localAddr;
    }

    public LiveData<String> getErrorInfo() {
        return errorInfo;
    }

    public LiveData<String> getNotifyInfo() { return notifyInfo; }

    public String getStatsInfo() {
        String str = null;
        if (tmioProxy != null) {
            str = tmioProxy.getStats();
        }

        if (str == null || str.isEmpty()) {
            return "";
        }

        try {
            JSONObject json = new JSONObject(str);
            StatsInfo statsInfo = StatsInfo.fromJson(json);
            return statsInfo.describe();
        } catch (JSONException exception) {
            return exception.toString();
        }
    }

    private void releaseCellularFileDesc() {
        try {
            if (null != cellularFileDescriptor) {
                cellularFileDescriptor.close();
                cellularFileDescriptor = null;
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private static class BaseStatsInfo {
        public long pktSent;
        public long pktRecv;
        public long pktSendLoss;
        public long pktRecvLoss;

        public static BaseStatsInfo fromJson(JSONObject json) throws JSONException {
            BaseStatsInfo baseStatsInfo = new BaseStatsInfo();
            baseStatsInfo.pktSent = json.getLong("pkt_sent");
            baseStatsInfo.pktRecv = json.getLong("pkt_recv");
            baseStatsInfo.pktSendLoss = json.getLong("pkt_sent_loss");
            baseStatsInfo.pktRecvLoss = json.getLong("pkt_recv_loss");
            return baseStatsInfo;
        }

        public double getSentLossRate() {
            if (pktSent == 0) {
                return 0;
            }
            return (double) pktSendLoss / pktSent;
        }

        public double getRecvLossRate() {
            if (pktRecv == 0) {
                return 0;
            }
            return (double) pktRecvLoss / pktRecv;
        }

        public String describe() {
            return "pktSent " + pktSent + ", pktRecv " + pktRecv + ", pktSentLoss " + pktSendLoss +
                    ", sentLossRate " +
                    String.format(Locale.ENGLISH, "%.2f%%", getSentLossRate() * 100);
        }
    }

    private static class StatsInfo {
        public BaseStatsInfo total = new BaseStatsInfo();
        public BaseStatsInfo current = new BaseStatsInfo();
        public long timestampMs;
        public double sentRateMbps;
        public double bandwidthMbps;
        public double rttMs;

        public static StatsInfo fromJson(JSONObject json) throws JSONException {
            JSONObject total = json.getJSONObject("total");
            JSONObject current = json.getJSONObject("current");

            StatsInfo statsInfo = new StatsInfo();
            statsInfo.total = BaseStatsInfo.fromJson(total);
            statsInfo.current = BaseStatsInfo.fromJson(current);

            statsInfo.timestampMs = json.getLong("timestamp_ms");
            statsInfo.sentRateMbps = json.getDouble("sent_rate_mbps");
            statsInfo.bandwidthMbps = json.getDouble("bandwidth_mbps");
            statsInfo.rttMs = json.getDouble("rtt_ms");
            return statsInfo;
        }

        public String describe() {
            return "Timestamp " + (timestampMs / 1000.0) + "\n\n" +
                    "Total " + total.describe() + "\n\n" +
                    "Current " + current.describe() + "\n\n" +
                    String.format(Locale.ENGLISH, "sentRateMbps %.3f, bandWidthMbps %.3f, RTT %.1f",
                    sentRateMbps, bandwidthMbps, rttMs);
        }
    }

    private TmioProxyListener proxyListener = new TmioProxyListener() {
        @Override
        public void onStart(String host, int port) {
            localAddr.postValue("tcp://" + host + ":" + port);
            releaseCellularFileDesc();
        }

        @Override
        public void onError(int errType, int errCode, String errMsg) {
            String type;
            switch (errType) {
                case TmioProxy.ERROR_TYPE_LOCAL_ERROR:
                    type = "local error";
                    break;
                case TmioProxy.ERROR_TYPE_REMOTE_ERROR:
                    type = "remote error";
                    break;
                default:
                    type = "unknown";
                    break;
            }
            String err = type + ": " + errMsg + ", " + errCode;
            errorInfo.postValue(err);
            releaseCellularFileDesc();
        }

        @Override
        public void onNotify(String notifyMsg) {
            String notify = "Notify" + ": " + notifyMsg + " is broken";
            notifyInfo.postValue(notify);
        }
    };
}