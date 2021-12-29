/**
 * Copyright (c) 2021 Tencent
 */

package com.tencent.xbright.tmio_demo.ffmpeg;

import android.util.Log;

import com.arthenica.mobileffmpeg.Config;
import com.arthenica.mobileffmpeg.ExecuteCallback;
import com.arthenica.mobileffmpeg.FFmpeg;

import static com.arthenica.mobileffmpeg.Config.RETURN_CODE_SUCCESS;
import static com.arthenica.mobileffmpeg.Config.RETURN_CODE_CANCEL;

public class FFmpegTask {
    private static final String TAG = "FFmpeg cmd task:";

    private long ffmpeg_execute_id = -1;

    public void ffmpegTestTask(String command) {
        // cancel old task first
        quitFFtask();

        ffmpeg_execute_id = FFmpeg.executeAsync(command, new ExecuteCallback() {
            @Override
            public void apply(final long executionId, final int returnCode) {
                if (returnCode == RETURN_CODE_SUCCESS) {
                    Log.i(Config.TAG, "Async command execution completed successfully.");
                } else if (returnCode == RETURN_CODE_CANCEL) {
                    Log.i(Config.TAG, "Async command execution cancelled by user.");
                } else {
                    Log.i(Config.TAG, String.format("Async command execution failed with returnCode=%d.", returnCode));
                }
            }
        });
    }

    public void quitFFtask() {
        if (ffmpeg_execute_id != -1) {
            FFmpeg.cancel(ffmpeg_execute_id);
            ffmpeg_execute_id = -1;
        }
    }
}
