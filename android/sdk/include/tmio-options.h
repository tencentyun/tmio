/**
 * Copyright (c) 2022 Tencent
 */

#pragma once

#include <stdint.h>

/**
 * optname and value for Tmio::setXXXOption()
 */

namespace tmio {
namespace base_options {
// value: bool
constexpr const char *const THREAD_SAFE_CHECK = "thread safe check";

constexpr const char *const TIMEOUT_MS = "timeout (ms)";

constexpr uint32_t FLAG_RECV = 1;
constexpr uint32_t FLAG_SEND = 2;
constexpr uint32_t FLAG_RECV_SEND = FLAG_RECV | FLAG_SEND;
constexpr const char *const RECV_SEND_FLAGS = "recv send flags";
}  // namespace base_options

// https://github.com/Haivision/srt/blob/master/docs/APISocketOptions.md
namespace srt_options {
// SRTO_SNDBUF
constexpr const char *const SEND_BUFFER_SIZE = "send buffer size (in bytes)";

// SRTO_RCVBUF
constexpr const char *const RECV_BUFFER_SIZE = "receive buffer size (in bytes)";

// SRTO_PAYLOADSIZE
constexpr const char *const PAYLOAD_SIZE = "srt maximum payload size (bytes)";
constexpr int PAYLOAD_SIZE_MAX = 1456;  // MTU(1500) - UDP.hdr(28) - SRT.hdr(16)
constexpr int PAYLOAD_SIZE_LIVE = 1316;  // = 188*7, recommended for MPEG TS

// SRTO_LATENCY
constexpr const char *const LATENCY = "srt latency (ms)";

// SRTO_MAXBW
// -1: infinite (the limit in Live Mode is 1 Gbps);
// 0: relative to input rate (see SRTO_INPUTBW);
// >0: absolute limit in B/s.
// default: -1
constexpr const char *const MAXBW = "Maximum bandwidth (bytes per second)";

// Recovery bandwidth overhead above input rate (see SRTO_INPUTBW), in
// percentage of the input rate. It is effective only if SRTO_MAXBW is
// set to 0.
//
// Sender: user configurable, default: 25%.
//
// Overhead is intended to give you extra bandwidth for the case when a
// packet has taken part of the bandwidth, but then was lost and has to
// be retransmitted. Therefore the effective maximum bandwidth should be
// appropriately higher than your stream's bitrate so that there's some
// room for retransmission, but still limited so that the retransmitted
// packets don't cause the bandwidth usage to skyrocket when larger
// groups of packets are lost
//
// Don't configure it too low and avoid 0 in the case when you have the
// SRTO_INPUTBW option set to 0 (automatic). Otherwise your stream will
// choke and break quickly at any rise in packet loss.
constexpr const char *BANDWIDTH_OVERHEAD_LIMIT =
    "srt recovery bandwidth overhead above input rate";

constexpr const char *const STREAM_ID = "srt stream id";

constexpr const char *const CONNECT_TIMEOUT =
    "Connect timeout(in milliseconds). Caller default: 3000, rendezvous (x 10)";

// SRTO_PBKEYLEN
// Encryption key length.
// Type: int32_t
//
// Possible values:
//
// 0 =PBKEYLEN (default value)
// 16 = AES-128 (effective value)
// 24 = AES-192
// 32 = AES-256
constexpr const char *const PBKEY_LEN = "Crypto key len in bytes";

// SRTO_PASSPHRASE
// Type: string
// Sets the passphrase for encryption. This enables encryption on this party (or
// disables it, if an empty passphrase is passed). The password must be minimum
// 10 and maximum 79 characters long.
constexpr const char *const PASSPHRASE =
    "Crypto PBKDF2 Passphrase. length [0,10..79] 0:disable crypto";

// SRTO_INPUTBW
// Type: int64_t
// This option is effective only if SRTO_MAXBW is set to 0 (relative). It
// controls the maximum bandwidth together with SRTO_OHEADBW option according to
// the formula: MAXBW = INPUTBW * (100 + OHEADBW) / 100.
// When this option is set to 0 (automatic) then the real INPUTBW value will be
// estimated from the rate of the input (cases when the application calls the
// srt_send* function) during transmission.
// Recommended: set this option to the anticipated bitrate of your live stream
// and keep the default 25% value for SRTO_OHEADBW.
constexpr const char *const INPUT_BW =
    "Estimated input bandwidth (bytes per second)";

constexpr const char *const MIN_INPUT_BW =
    "Minimum value of input bandwidth estimation (bytes per second)";

// SRTO_TLPKTDROP
// Type: bool
constexpr const char *const TOO_LATE_PACKET_DROP = "Too-late Packet Drop";

// SRTO_MINVERSION
// Type: int32_t
constexpr const char *const MIN_VERSION =
    "Minimum SRT version that is required from the peer";

// SRT_TRANSTYPE
constexpr int TRANSTYPE_LIVE = 0;
constexpr int TRANSTYPE_FILE = 1;
constexpr const char *const TRANSTYPE = "The transmission type for the socket";

// SRT_CONGESTIONTYPE
// The bandwidth congestion control option defaults to match the trans type(SRT_TRANSTYPE).
// TRANSTYPE_LIVE->"live", TRANSTYPE_FILE->"file", “live_bbr” is an extension based on live mode
// When the TRANSTYPE = TRANSTYPE_LIVE, you can choose to configure it as “live_bbr”
// or use the default configuration of "live"
constexpr const char *const FILE_CONGCTL = "file";
constexpr const char *const LIVE_CONGCTL = "live";
constexpr const char *const LIVE_BBR_RATESAMPLE = "live_bbr";
constexpr const char *const CONGESTIONTYPE = "The congestion control algorithm";

// SRTO_LINGER
// Type: int
constexpr const char *const LINGER =
    "Number of seconds that the socket waits for unsent data when closing";

// SRTO_TSBPDMODE
// Type: bool
constexpr const char *const TSBPDMODE = "TsbPd";
}  // namespace srt_options

namespace rist_options {
// Type: int
constexpr const char *const BUFFER_SIZE = "buffer size in ms";

// Type: int
constexpr int PROFILE_SIMPLE = 0;
constexpr int PROFILE_MAIN = 1;
constexpr int PROFILE_ADVANCED = 2;
constexpr const char *const PROFILE = "profile";
}  // namespace rist_options

}  // namespace tmio