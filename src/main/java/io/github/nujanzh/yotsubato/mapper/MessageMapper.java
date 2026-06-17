package io.github.nujanzh.yotsubato.mapper;

import io.github.nujanzh.yotsubato.dto.message.MessageResponse;
import io.github.nujanzh.yotsubato.dto.message.MessageSender;
import io.github.nujanzh.yotsubato.dto.message.ReplyPreview;
import io.github.nujanzh.yotsubato.model.message.Message;
import io.github.nujanzh.yotsubato.model.user.User;
import java.util.List;
import java.util.Map;
import java.util.UUID;

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
                message.getEditedAt(),
                replyPreview,
                null);
    }

    public static List<MessageResponse> toMessageResponseList(
            List<Message> messages, Map<UUID, User> senders, Map<UUID, Message> parents) {
        return messages.stream().map(m -> toMessageResponse(m, senders, parents)).toList();
    }

    private static MessageResponse toMessageResponse(
            Message m, Map<UUID, User> senders, Map<UUID, Message> parents) {
        User sender = senders.get(m.getSender().getId());
        if (sender == null) {
            throw new IllegalStateException("Sender not found for message: " + m.getId());
        }

        ReplyPreview preview = resolveReplyPreview(m, parents);
        MessageSender messageSender = new MessageSender(sender.getId(), sender.getUsername());

        return toMessageResponse(m, messageSender, preview);
    }

    public static ReplyPreview resolveReplyPreview(Message m, Map<UUID, Message> parents) {
        if (m.getReplyToId() == null) {
            return null;
        }
        if (parents.containsKey(m.getReplyToId())) {
            return ReplyPreview.from(parents.get(m.getReplyToId()));
        }
        return ReplyPreview.deleted(m.getReplyToId());
    }
}
