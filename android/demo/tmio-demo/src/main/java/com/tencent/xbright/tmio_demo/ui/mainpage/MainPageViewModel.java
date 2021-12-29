package com.tencent.xbright.tmio_demo.ui.mainpage;

import android.app.Application;
import android.content.Context;
import android.net.Network;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.tencent.xbright.tmio_demo.network_util.NetworkUtil;

public class MainPageViewModel extends AndroidViewModel {
    private MutableLiveData<String> cellularNetworkInfo = new MutableLiveData<String>();

    public MainPageViewModel(@NonNull Application application) {
        super(application);
    }

    public LiveData<String> getCellularNetwork() {
        return cellularNetworkInfo;
    }

    public void requestCellularNetwork() {
        Context context = getApplication().getApplicationContext();
        NetworkUtil.requestCellular(context, new NetworkUtil.NetworkCallback() {
            @Override
            public void onSuccess(Network network) {
                cellularNetworkInfo.postValue("开启移动网络成功");
            }

            @Override
            public void onFailure(Network network, String msg) {
                String error = "开启移动网络失败";
                if (network != null)
                    error += "，" + network.toString();
                if (msg != null && !msg.isEmpty())
                    error += "，" + msg;
                cellularNetworkInfo.postValue(error);
            }
        });
    }
}
