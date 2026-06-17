package io.github.nujanzh.yotsubato.web.controller;

import io.github.nujanzh.yotsubato.dto.message.MessageResponse;
import io.github.nujanzh.yotsubato.dto.message.RoomEvent;
import io.github.nujanzh.yotsubato.dto.message.SendMessageRequest;
import io.github.nujanzh.yotsubato.dto.message.StompError;
import io.github.nujanzh.yotsubato.exception.ResourceNotFoundException;
import io.github.nujanzh.yotsubato.security.userdetails.AuthenticatedPrincipal;
import io.github.nujanzh.yotsubato.service.MessageService;
import jakarta.validation.Valid;
import java.security.Principal;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.*;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;

@Slf4j
@Controller
public class ChatController {

    private final MessageService messageService;
    private final SimpMessagingTemplate template;

    public ChatController(MessageService messageService, SimpMessagingTemplate template) {
        this.messageService = messageService;
        this.template = template;
    }

    @MessageMapping("/rooms/{roomId}/message")
    public void sendMessage(
            @DestinationVariable UUID roomId,
            @Payload @Valid SendMessageRequest request,
            Principal principal,
            @Header(name = "clientMessageId", required = false) String clientMessageId) {

        AuthenticatedPrincipal user =
                (AuthenticatedPrincipal) ((Authentication) principal).getPrincipal();

        MessageResponse response = messageService.createMessage(roomId, user.userId(), request);

        template.convertAndSend(
                "/topic/rooms/" + roomId,
                new RoomEvent.MessageCreatedEvent(response.withClientMessageId(clientMessageId)));
    }

    @MessageExceptionHandler(ResourceNotFoundException.class)
    @SendToUser(destinations = "/queue/error", broadcast = false)
    public StompError handleResourceNotFoundException(
            ResourceNotFoundException ex,
            @Header(name = "clientMessageId", required = false) String clientMessageId) {
        String message = "Resource " + ex.getResourceName() + " not found";
        return new StompError(404, message, clientMessageId);
    }

    @MessageExceptionHandler(Exception.class)
    @SendToUser(destinations = "/queue/error", broadcast = false)
    public StompError handleException(
            Exception ex,
            @Header(name = "clientMessageId", required = false) String clientMessageId) {
        log.error("Unhandled messaging exception", ex);
        return new StompError(500, "An unexpected error occurred", clientMessageId);
    }
}
