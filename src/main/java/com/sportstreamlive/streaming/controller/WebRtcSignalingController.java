package com.sportstreamlive.streaming.controller;

import com.sportstreamlive.streaming.model.WebRtcSignal;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.util.StringUtils;

/**
 * Signaling WebRTC sobre STOMP.
 *
 * Cliente envia: /app/webrtc/{streamId}/signal
 * Cliente escucha: /topic/webrtc/{streamId}
 */
@Slf4j
@Controller
public class WebRtcSignalingController {

    private final SimpMessagingTemplate messagingTemplate;

    public WebRtcSignalingController(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    @MessageMapping("/webrtc/{streamId}/signal")
    public void signal(@DestinationVariable String streamId,
                       @Payload WebRtcSignal signal) {
        if (!StringUtils.hasText(streamId) || signal == null || !StringUtils.hasText(signal.getType())) {
            return;
        }

        signal.setStreamId(streamId);
        messagingTemplate.convertAndSend("/topic/webrtc/" + streamId, signal);

        if (log.isDebugEnabled()) {
            log.debug("WebRTC signal [{}] stream={} from={} to={}",
                    signal.getType(),
                    streamId,
                    signal.getSenderUserId(),
                    signal.getTargetUserId());
        }
    }
}
