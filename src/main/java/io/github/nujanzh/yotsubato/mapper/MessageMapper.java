package io.github.nujanzh.yotsubato.mapper;

import io.github.nujanzh.yotsubato.dto.message.MessageResponse;
import io.github.nujanzh.yotsubato.dto.message.MessageSender;
import io.github.nujanzh.yotsubato.dto.message.ReplyPreview;
import io.github.nujanzh.yotsubato.model.message.Message;

public class MessageMapper {
    private MessageMapper() {}

    public static MessageResponse toMessageResponse(
            Message message, MessageSender sender, ReplyPreview replyPreview) {
        // We're setting clientId in Controler layer
        return new MessageResponse(
                message.getId(),
                message.getRoom().getId(),
                sender,
                message.getContent(),
                message.getType(),
                message.getSentAt(),
                replyPreview,
                null);
    }
}
