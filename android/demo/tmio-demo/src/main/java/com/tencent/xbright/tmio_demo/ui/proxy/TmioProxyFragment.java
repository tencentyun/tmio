package com.tencent.xbright.tmio_demo.ui.proxy;

import android.app.Application;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.tencent.xbright.tmio_demo.R;
import com.tencent.xbright.tmio_demo.TmioProxy;
import com.tencent.xbright.tmio_demo.config.ConfigSharePref;
import com.tencent.xbright.tmio_demo.ffmpeg.FFmpegTask;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;

public class TmioProxyFragment extends Fragment {
    private static final String TAG = "TmioProxyFragment";

    private FFmpegTask ffmpegCmd;
    private TmioProxyViewModel tmioProxyViewModel;
    private EditText localAddrView;
    private EditText remoteAddrView;
    private Button startPushBtn;
    private Button startProxyBtn;
    private TextView errorView;
    private TextView statsView;
    private Spinner protocolSpinner;

    private Button addWifiLinkBtn;
    private Button addCellLinkBtn;
    private EditText playbackAddr;
    private VideoView videoView;
    private Button startPlayBtn;
    private boolean playback;

    private Handler handler;
    private String realLocalUrl;

    private String remoteUrlRtmp;
    private String remoteUrlTs;
    private String playUrl;
    private String localUrlRtmp;
    private String localUrlTs;

    private int bondingMode = 0;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        handler = new Handler(Looper.myLooper());
        Application app = this.getActivity().getApplication();
        tmioProxyViewModel =
                new ViewModelProvider.AndroidViewModelFactory(app).create(TmioProxyViewModel.class);
        View root = inflater.inflate(R.layout.fragment_proxy, container, false);

        bindView(root);

        setupUrl();

        tmioProxyViewModel.getLocalAddr().observe(getViewLifecycleOwner(), this::onStart);
        tmioProxyViewModel.getErrorInfo().observe(getViewLifecycleOwner(), this::onError);
        tmioProxyViewModel.getNotifyInfo().observe(getViewLifecycleOwner(), this::onNotify);


        startProxyBtn.setOnClickListener(v -> startProxy());
        startPushBtn.setOnClickListener(v -> startPush());
        startPlayBtn.setOnClickListener(this::onPlaybackBtn);
        addWifiLinkBtn.setOnClickListener(v -> addWifiLink());
        addCellLinkBtn.setOnClickListener(v -> addCellLink());

        protocolSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String protocol = protocolSpinner.getItemAtPosition(position).toString();
                Log.i("onItemSelected", "protocol: " + protocol + ", position:" + position + ", id:" +id);
                if (protocol.equals("RTMP")) {
                    remoteAddrView.setText(remoteUrlRtmp);
                    localAddrView.setText(localUrlRtmp);
                } else {
                    remoteAddrView.setText(remoteUrlTs);
                    localAddrView.setText(localUrlTs);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        ffmpegCmd = new FFmpegTask();
        return root;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
    }

    private void startProxy() {
        errorView.setText("");
        statsView.setText("");

        String localUrl = localAddrView.getText().toString();
        String remotePushUrl = remoteAddrView.getText().toString();
        String protocol = protocolSpinner.getSelectedItem().toString();

        ConfigSharePref.updateConfig("push_remote_url", remotePushUrl);
        ConfigSharePref.updateConfig(protocol.equals("TS") ? "ts_local_url" : "rtmp_local_url", localUrl);
        updateUrl(remotePushUrl);

        String remoteUrl = protocol.equals("TS") ? remoteUrlTs : remoteUrlRtmp;
        remoteAddrView.setText(remoteUrl);
        tmioProxyViewModel.start(localUrl, remoteUrl, protocol, bondingMode);
        startProxyBtn.setText(R.string.stop_proxy);
        startProxyBtn.setOnClickListener(v -> stopProxy());
    }

    private void stopProxy() {
        startProxyBtn.setText(R.string.start_proxy);
        startProxyBtn.setOnClickListener(vv -> startProxy());
        tmioProxyViewModel.stop();
    }

    private String generateLocalRtmpPushUrl(String remoteUrl, String localUrl){
        String txHost = null;
        String txSecret = null;
        String txTime = null;
        String txLiveId = null;

        if (!remoteUrl.startsWith("srt:") || remoteUrl.indexOf("streamid=") == -1) {
            return null;
        }

        if (!localUrl.startsWith("tcp://")) {
            return null;
        }

        String[] remoteStrArray = remoteUrl.split(",");
        for (int i = 0; i < remoteStrArray.length; i++) {
            if (remoteStrArray[i].startsWith("txSecret=")) {
                txSecret = remoteStrArray[i];
            } else if (remoteStrArray[i].startsWith("txTime=")) {
                txTime = remoteStrArray[i];
            } else if (remoteStrArray[i].startsWith("r=")) {
                txLiveId = remoteStrArray[i].replaceFirst("r=", "/");
            } else if (remoteStrArray[i].startsWith("srt:")) {
                if(remoteStrArray[i].indexOf("streamid=#!::h=") == -1) {
                    return null;
                }
                txHost = remoteStrArray[i].substring(remoteStrArray[i].indexOf("streamid=#!::h=")).replaceFirst(
                        "streamid=#!::h=", "txHost=");
            } else {
                return null;
            }
        }

        if (TextUtils.isEmpty(txHost) || TextUtils.isEmpty(txSecret)
            || TextUtils.isEmpty(txTime) || TextUtils.isEmpty(txLiveId)) {
            return null;
        }
        return "rtmp" + localUrl.substring(localUrl.indexOf("://")) + txLiveId + "?" + txSecret + "&" + txTime + "&" + txHost;
    }

    private void startPush() {
        String localUrl = localAddrView.getText().toString();
        String protocol = protocolSpinner.getSelectedItem().toString();

        if (protocol.equals("RTMP")) {
            String remoteUrl = remoteAddrView.getText().toString();
            String rtmpUrl = generateLocalRtmpPushUrl(remoteUrl, localUrl);
            if (rtmpUrl != null && !rtmpUrl.equals("")) {
                ffmpeg_push(rtmpUrl, protocol);
            } else {
                errorView.setText("url format error: " + getResources().getString(R.string.srt_url_hint_ex));
                return ;
            }
        } else {
            ffmpeg_push(localUrl, protocol);
        }
        startPushBtn.setText(R.string.stop_push);
        startPushBtn.setOnClickListener(v -> stopPush());
    }

    private void addWifiLink() {
        String remoteUrl = remoteAddrView.getText().toString();
        tmioProxyViewModel.addWifiLink(remoteUrl);
    }

    private void addCellLink() {
        String remoteUrl = remoteAddrView.getText().toString();
        tmioProxyViewModel.addCellNetLink(remoteUrl);
    }

    private void stopPush() {
        ffmpegCmd.quitFFtask();
        startPushBtn.setText(R.string.start_push);
        startPushBtn.setOnClickListener(vv -> startPush());
    }

    private void onStart(String msg) {
        realLocalUrl = msg;
        localAddrView.setText(realLocalUrl);
        startStats();
    }

    private void onError(String msg) {
        errorView.setText(msg);
        stopProxy();
        stopPush();
    }

    private  void onNotify(String msg) {
        Toast.makeText(getContext(), msg, Toast.LENGTH_SHORT).show();
    }

    private void startStats() {
        String stats = tmioProxyViewModel.getStatsInfo();
        if (!stats.isEmpty()) {
            statsView.setText(stats);
            handler.postDelayed(this::startStats, 1000);
        }
    }

    private void onPlaybackBtn(View v) {
        if (playback) {
            startPlayBtn.setText(R.string.start_playback);
            playback = false;
            videoView.stopPlayback();
        } else {
            startPlayBtn.setText(R.string.stop_playback);
            playback = true;
            String playbackUrl = playbackAddr.getText().toString();
            Log.i(TAG, "playback url " + playbackUrl);
            videoView.setVideoPath(playbackUrl);
            videoView.start();
        }
    }

    /**
     * 读取流到文件中
     *
     * @param context
     * @param resourceId
     * @param file
     * @return
     * @throws IOException
     */
    private static File readRawToFile(Context context, int resourceId, File file) throws IOException {
        final InputStream inputStream = context.getResources().openRawResource(resourceId);
        if (file.exists()) {
            file.delete();
        }
        final FileOutputStream outputStream = new FileOutputStream(file);
        try {
            final byte[] buffer = new byte[1024];
            int readSize;

            while ((readSize = inputStream.read(buffer)) > 0) {
                outputStream.write(buffer, 0, readSize);
            }
        } catch (final IOException e) {
            Log.e("yyyy", "Saving raw resource failed.", e);
            return file;
        } finally {
            inputStream.close();
            outputStream.flush();
            outputStream.close();
            return file;
        }
    }

    public void ffmpeg_push(String msg, String protocol) {
        String strUrl = msg;
        File cache = getActivity().getApplicationContext().getCacheDir();

        File tempFile = new File(cache, "clock_av_tmp" + ".flv");
        try {
            tempFile = readRawToFile(getActivity().getApplicationContext(), R.raw.clock_av, tempFile);
        } catch (IOException e) {
            e.printStackTrace();
        }

        String file_path = tempFile.getAbsolutePath();
        String format = protocol.equals("RTMP") ? "flv " : "mpegts ";
        Log.i(TAG, "ffmpeg_push url " + strUrl + ", file_path:" + file_path);
        String pushcmd = "-re -loglevel trace -stream_loop -1 -i " + file_path + " -c copy -f " + format + strUrl;
        ffmpegCmd.ffmpegTestTask(pushcmd);
    }

    private void bindView(View root) {
        localAddrView = root.findViewById(R.id.local_addr);
        remoteAddrView = root.findViewById(R.id.remote_addr);
        startPushBtn = root.findViewById(R.id.start_push_btn);
        startProxyBtn = root.findViewById(R.id.start_proxy_btn);
        addWifiLinkBtn = root.findViewById(R.id.add_new_wifilink_btn);
        addCellLinkBtn = root.findViewById(R.id.add_new_celllink_btn);

        errorView = root.findViewById(R.id.error_view);
        statsView = root.findViewById(R.id.stats_view);

        playbackAddr = root.findViewById(R.id.playback_url);
        videoView = root.findViewById(R.id.video_view);
        startPlayBtn = root.findViewById(R.id.start_play_btn);
        protocolSpinner = root.findViewById(R.id.protocol_spinner);

        RadioGroup radioGroup = root.findViewById(R.id.bonding_mode_container);
        radioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                if (checkedId == R.id.bonding_backup)
                    bondingMode = TmioProxy.TMIO_BONDING_MODE_BACKUP;
                else if (checkedId == R.id.bonding_broadcast) {
                    bondingMode = TmioProxy.TMIO_BONDING_MODE_BROADCAST;
                } else {
                    bondingMode = TmioProxy.TMIO_BONDING_MODE_DEFAULT;
                }
            }
        });
    }

    private String parsePlayUrlByPushUrl(String remotePushUrl) {
        String playUrl = "";
        String txLiveId = "";

        if (!remotePushUrl.startsWith("srt:") || remotePushUrl.indexOf("streamid=") == -1) {
            return null;
        }

        String[] remoteStrArray = remotePushUrl.split(",");
        for (int i = 0; i < remoteStrArray.length; i++) {
            if (remoteStrArray[i].startsWith("r=")) {
                txLiveId = remoteStrArray[i].replaceFirst("r=", "/");
            }
        }

        if (!txLiveId.equals("")) {
            playUrl = "https://" + getResources().getString(R.string.playback_host_default)
                    + txLiveId + ".m3u8";
        }

        return playUrl;
    }

    private String replacePushUrlPort(String remotePushUrl, int port) {
        String remoteUrl = getResources().getString(R.string.remote_url_hint);
        if (!remotePushUrl.startsWith("srt:") || remotePushUrl.indexOf("streamid=") == -1) {
            return remoteUrl;
        }

        try {
            URI uri = new URI(remotePushUrl);
            if (uri.getPort() == port) {
                return remotePushUrl;
            } else {
                return new URI(uri.getScheme(), uri.getUserInfo(), uri.getHost(), port,
                        uri.getPath(), uri.getQuery(), uri.getFragment()).toString();
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return remoteUrl;
    }

    private void updateUrl(String remotePushUrl) {
        remoteUrlRtmp = replacePushUrlPort(remotePushUrl, 3570);
        remoteUrlTs = replacePushUrlPort(remotePushUrl, 9000);
        playUrl = parsePlayUrlByPushUrl(remotePushUrl);
        playbackAddr.setText(playUrl);
    }

    private void setupUrl() {
        String remotePushUrl = ConfigSharePref.getConfigString("push_remote_url",
                "");
        updateUrl(remotePushUrl);
        localUrlRtmp = ConfigSharePref.getConfigString("rtmp_local_url",
                getResources().getString(R.string.local_addr_default));
        localUrlTs = ConfigSharePref.getConfigString("ts_local_url",
                getResources().getString(R.string.local_addr_ts));
    }
}