/**
 * Copyright (c) 2022 Tencent
 */

#pragma once

#include <stdint.h>

#include <string>

#include "tmio-common.h"

namespace tmio {

struct TMIO_EXTERN PerfStats {
    struct BaseInfo {
        // number of sent data packets, including retransmissions
        int64_t pkt_sent;
        // number of received packets
        int64_t pkt_recv;

        // number of lost packets (senter side)
        int64_t pkt_sent_loss;
        // number of lost packets (receiver side)
        int64_t pkt_recv_loss;

        // number of retransmitted packets
        int64_t pkt_retrans;

        // number of sent ACK packets
        int64_t pkt_sent_ack;
        // number of received ACK packets
        int64_t pkt_recv_ack;
        // number of sent NAK packets
        int64_t pkt_sent_nak;
        // number of received NAK packets
        int64_t pkt_recv_nak;

        // number of too-late-to-sent dropped packets
        int64_t pkt_sent_drop;
        // number of too-late-to play missing packets
        int64_t pkt_recv_drop;

        // number of sent data bytes, including retransmissions
        int64_t byte_sent;
        // number of received bytes
        int64_t byte_recv;

        // number of lost bytes
        int64_t byte_recv_loss;

        // number of retransmitted bytes
        int64_t byte_retrans;

        // number of too-late-to-sent dropped bytes
        int64_t byte_sent_drop;
        // number of too-late-to play missing bytes
        int64_t byte_recv_drop;
    };

    // statistic since the beginning
    BaseInfo total;

    // statistic since last time of get stats
    BaseInfo current;

    // time since start, in milliseconds
    int64_t timestamp_ms;

    int64_t bytes_in_flight;
    // senting rate in Mb/s
    double sent_rate_mbps;
    // receiving rate in Mb/s
    double recv_rate_mbps;
    // estimated bandwidth, in Mb/s
    double bandwidth_mbps;
    // RTT, in milliseconds
    double rtt_ms;

    double delivery_rate_mbps;
    double sent_buffer_watermark;
    double recv_buffer_watermark;

    /**
     * dump field as json
     *
     * @param compat true for single line, false for multiple lines
     */
    std::string toStr(bool compat) const;
};

}  // namespace tmio