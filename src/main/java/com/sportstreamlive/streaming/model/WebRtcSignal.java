package com.sportstreamlive.streaming.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Mensaje de signaling WebRTC para coordinar offer/answer/ice
 * entre un emisor y multiples viewers sobre STOMP.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class WebRtcSignal {

    /** JOIN, OFFER, ANSWER, ICE, LEAVE */
    private String type;

    /** streamId asociado al en vivo */
    private String streamId;

    /** userId del emisor del mensaje de signaling */
    private String senderUserId;

    /** userId destino. null/blank => broadcast a todos */
    private String targetUserId;

    /** SDP para OFFER/ANSWER */
    private String sdp;

    /** ICE candidate */
    private String candidate;

    /** ICE sdpMid */
    private String sdpMid;

    /** ICE sdpMLineIndex */
    private Integer sdpMLineIndex;
}
