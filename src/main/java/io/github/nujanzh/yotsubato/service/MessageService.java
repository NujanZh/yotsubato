package io.github.nujanzh.yotsubato.service;

import io.github.nujanzh.yotsubato.dto.message.MessageResponse;
import io.github.nujanzh.yotsubato.dto.message.MessageSender;
import io.github.nujanzh.yotsubato.dto.message.ReplyPreview;
import io.github.nujanzh.yotsubato.dto.message.SendMessageRequest;
import io.github.nujanzh.yotsubato.exception.MessageNotFoundException;
import io.github.nujanzh.yotsubato.exception.RoomNotFoundException;
import io.github.nujanzh.yotsubato.mapper.MessageMapper;
import io.github.nujanzh.yotsubato.model.message.Message;
import io.github.nujanzh.yotsubato.model.room.Room;
import io.github.nujanzh.yotsubato.model.user.User;
import io.github.nujanzh.yotsubato.repository.message.MessageRepository;
import io.github.nujanzh.yotsubato.repository.room.RoomMemberRepository;
import io.github.nujanzh.yotsubato.repository.room.RoomRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class MessageService {

    private final MessageRepository messageRepository;
    private final RoomRepository roomRepository;
    private final RoomMemberRepository roomMemberRepository;
    private final UserService userService;

    public MessageService(
            MessageRepository messageRepository,
            RoomRepository roomRepository,
            RoomMemberRepository roomMemberRepository,
            UserService userService) {
        this.messageRepository = messageRepository;
        this.roomRepository = roomRepository;
        this.roomMemberRepository = roomMemberRepository;
        this.userService = userService;
    }

    @Transactional
    public MessageResponse createMessage(UUID roomId, UUID callerId, SendMessageRequest request) {
        Room room =
                roomRepository
                        .findById(roomId)
                        .orElseThrow(() -> new RoomNotFoundException("Room not found: " + roomId));

        roomMemberRepository
                .findByRoomIdAndUserId(roomId, callerId)
                .orElseThrow(() -> new RoomNotFoundException("Room not found: " + roomId));

        Message message = new Message();

        ReplyPreview replyPreview = null;

        if (request.replyToId() != null) {
            Message replyMessage =
                    messageRepository
                            .findByIdAndRoomId(request.replyToId(), roomId)
                            .orElseThrow(
                                    () ->
                                            new MessageNotFoundException(
                                                    "Reply message not found: "
                                                            + request.replyToId()));

            message.setReplyToId(replyMessage.getId());
            replyPreview =
                    new ReplyPreview(
                            replyMessage.getId(),
                            replyMessage.getSender().getUsername(),
                            replyMessage.getContent());
        }

        User sender = userService.getById(callerId);
        MessageSender messageSender = new MessageSender(sender.getId(), sender.getUsername());

        message.setSender(sender);
        message.setRoom(room);
        message.setContent(request.content());
        message.setType(request.type());
        Message savedMessage = messageRepository.saveAndFlush(message);

        return MessageMapper.toMessageResponse(savedMessage, messageSender, replyPreview);
    }
}
