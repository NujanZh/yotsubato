package io.github.nujanzh.yotsubato.web.controller;

import io.github.nujanzh.yotsubato.dto.message.MessageResponse;
import io.github.nujanzh.yotsubato.dto.message.SendMessageRequest;
import io.github.nujanzh.yotsubato.dto.message.StompError;
import io.github.nujanzh.yotsubato.exception.ResourceNotFoundException;
import io.github.nujanzh.yotsubato.security.userdetails.AuthenticatedPrincipal;
import io.github.nujanzh.yotsubato.service.MessageService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.handler.annotation.*;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;

import java.util.UUID;

@Slf4j
@Controller
public class ChatController {

    private final MessageService messageService;
    private final SimpMessagingTemplate template;

    public ChatController(MessageService messageService, SimpMessagingTemplate template) {
        this.messageService = messageService;
        this.template = template;
    }

    @MessageMapping("/room/{roomId}/message")
    public void sendMessage(
            @DestinationVariable UUID roomId,
            @Payload @Valid SendMessageRequest request,
            @AuthenticationPrincipal AuthenticatedPrincipal principal,
            @Header(required = false) String clientMessageId) {

        MessageResponse response =
                messageService.createMessage(roomId, principal.userId(), request);

        template.convertAndSend(
                "/topic/room/" + roomId, response.withClientMessageId(clientMessageId));
    }

    @MessageExceptionHandler(ResourceNotFoundException.class)
    @SendToUser(destinations = "/queue/error", broadcast = false)
    public StompError handleResourceNotFoundException(
            ResourceNotFoundException ex, @Header(required = false) String clientMessageId) {
        return new StompError(HttpStatus.NOT_FOUND.value(), ex.getMessage(), clientMessageId);
    }

    @MessageExceptionHandler(Exception.class)
    @SendToUser(destinations = "/queue/error", broadcast = false)
    public StompError handleException(
            Exception ex, @Header(required = false) String clientMessageId) {
        log.error("Unhandled messaging exception", ex);
        return new StompError(
                HttpStatus.INTERNAL_SERVER_ERROR.value(), ex.getMessage(), clientMessageId);
    }
}
