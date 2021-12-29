package com.tencent.xbright.tmio_demo.ui.mainpage;

import android.app.Application;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.tencent.xbright.tmio_demo.R;
import com.tencent.xbright.tmio_demo.TmioDemo;
import com.tencent.xbright.tmio_demo.network_util.NetworkUtil;

import java.util.ArrayList;

public class MainPageFragment extends Fragment {
    private Button requestCelluarBtn;
    private TextView celluarText;
    private Button requestNetworkCardInfoBtn;
    private TextView networkCardInfoText;

    MainPageViewModel viewModel;

    public MainPageFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        Application app = this.getActivity().getApplication();
        viewModel = new ViewModelProvider.AndroidViewModelFactory(app).create(MainPageViewModel.class);
        View view = inflater.inflate(R.layout.fragment_mainpage, container, false);
        TextView sdkVersion = view.findViewById(R.id.text_sdk_version);
        sdkVersion.setText("Tmio SDK版本：" + TmioDemo.getTmioVersion());

        requestNetworkCardInfoBtn = view.findViewById(R.id.button_req_netcard_info);
        networkCardInfoText = view.findViewById(R.id.text_netcard_info);
        // 自动获取网络信息，点击button时更新
        getNetworkCardInfo();
        requestNetworkCardInfoBtn.setOnClickListener((View btn) -> {
            getNetworkCardInfo();
        });

        requestCelluarBtn = view.findViewById(R.id.button_req_celluar);
        requestCelluarBtn.setOnClickListener((View btn) -> {
            viewModel.requestCellularNetwork();
        });
        celluarText = view.findViewById(R.id.text_celluar_network);
        viewModel.getCellularNetwork().observe(getViewLifecycleOwner(), (String info) -> {
            celluarText.setText(info);
        });
        viewModel.requestCellularNetwork();

        return view;
    }

    private void getNetworkCardInfo() {
        ArrayList<String> interfaces = NetworkUtil.getAllNetInterface();
        String str = TextUtils.join("\n", interfaces);
        networkCardInfoText.setText(str);
    }
}