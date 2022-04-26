/**
 * Copyright (c) 2022 Tencent
 */

#pragma once

#include "tmio.h"

namespace tmio {

class SrtPreset final {
 public:
    SrtPreset() = delete;
    ~SrtPreset() = delete;

    // 20 Mbps
    static constexpr int max_bandwidth = (20 << 20) / 8;

    static constexpr int buf_size = 2 << 20;

    static void rtmp(Tmio *tmio, bool isGroup = false) {
        tmio->setBoolOption(srt_options::TOO_LATE_PACKET_DROP, false);
        tmio->setIntOption(srt_options::PAYLOAD_SIZE,
                           srt_options::PAYLOAD_SIZE_LIVE);
        tmio->setIntOption(srt_options::LATENCY, 0);
        tmio->setBoolOption(srt_options::TSBPDMODE, false);
        commonSet(tmio);
    }

    static void mpegTsLossless(Tmio *tmio) {
        tmio->setBoolOption(srt_options::TOO_LATE_PACKET_DROP, false);
        tmio->setIntOption(srt_options::PAYLOAD_SIZE,
                           srt_options::PAYLOAD_SIZE_LIVE);
        tmio->setIntOption(srt_options::LATENCY, 0);
        tmio->setBoolOption(srt_options::TSBPDMODE, false);
        tmio->setIntOption(srt_options::MAXBW, max_bandwidth);
        commonSet(tmio);
    }

    static void mpegTsFixedLatency(Tmio *tmio, unsigned latency_ms = 120) {
        tmio->setBoolOption(srt_options::TOO_LATE_PACKET_DROP, true);
        tmio->setIntOption(srt_options::PAYLOAD_SIZE,
                           srt_options::PAYLOAD_SIZE_LIVE);
        tmio->setIntOption(srt_options::LATENCY, latency_ms);
        tmio->setBoolOption(srt_options::TSBPDMODE, true);
        commonSet(tmio);
    }

 private:
    static void commonSet(Tmio *tmio) {
        tmio->setIntOption(srt_options::MAXBW, max_bandwidth);
        tmio->setIntOption(srt_options::SEND_BUFFER_SIZE, buf_size);
        tmio->setIntOption(srt_options::RECV_BUFFER_SIZE, buf_size);
    }
};

}  // namespace tmio