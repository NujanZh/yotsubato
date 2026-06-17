package io.github.nujanzh.yotsubato.service;

import io.github.nujanzh.yotsubato.dto.message.*;
import io.github.nujanzh.yotsubato.exception.MessageNotFoundException;
import io.github.nujanzh.yotsubato.exception.RoomAccessDeniedException;
import io.github.nujanzh.yotsubato.exception.RoomNotFoundException;
import io.github.nujanzh.yotsubato.mapper.MessageMapper;
import io.github.nujanzh.yotsubato.model.message.Message;
import io.github.nujanzh.yotsubato.model.room.MemberRole;
import io.github.nujanzh.yotsubato.model.room.Room;
import io.github.nujanzh.yotsubato.model.user.User;
import io.github.nujanzh.yotsubato.repository.message.MessageRepository;
import io.github.nujanzh.yotsubato.repository.room.RoomMemberRepository;
import io.github.nujanzh.yotsubato.repository.room.RoomRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MessageService {

    private final MessageRepository messageRepository;
    private final RoomRepository roomRepository;
    private final RoomMemberRepository roomMemberRepository;
    private final UserService userService;
    private final Clock clock;

    private static final int MAX_HISTORY_LIMIT = 100;

    public MessageService(
            MessageRepository messageRepository,
            RoomRepository roomRepository,
            RoomMemberRepository roomMemberRepository,
            UserService userService,
            Clock clock) {
        this.messageRepository = messageRepository;
        this.roomRepository = roomRepository;
        this.roomMemberRepository = roomMemberRepository;
        this.userService = userService;
        this.clock = clock;
    }

    @Transactional
    public MessageResponse createMessage(UUID roomId, UUID callerId, SendMessageRequest request) {
        if (!roomMemberRepository.existsByRoomIdAndUserId(roomId, callerId)) {
            throw new RoomNotFoundException("Room not found: " + roomId);
        }

        Room room = roomRepository.getReferenceById(roomId);

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
            replyPreview = ReplyPreview.from(replyMessage);
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

    @Transactional(readOnly = true)
    public MessageHistoryResponse getRoomHistory(
            UUID roomId, UUID callerId, MessageCursor cursor, int limit) {

        if (!roomMemberRepository.existsByRoomIdAndUserId(roomId, callerId)) {
            throw new RoomNotFoundException("Room not found: " + roomId);
        }

        int safe = Math.clamp(limit, 1, MAX_HISTORY_LIMIT);
        Limit fetch = Limit.of(safe + 1);

        List<Message> results =
                cursor == null
                        ? messageRepository.findByRoomIdOrderBySentAtDescIdDesc(roomId, fetch)
                        : messageRepository.findRoomMessagesBefore(
                                roomId, cursor.sentAt(), cursor.lastId(), fetch);

        boolean hasMore = results.size() > safe;
        if (hasMore) {
            results = results.subList(0, safe);
        }

        Set<UUID> senderIds =
                results.stream().map(m -> m.getSender().getId()).collect(Collectors.toSet());
        Map<UUID, User> senders =
                userService.getAllByIds(senderIds).stream()
                        .collect(Collectors.toMap(User::getId, Function.identity()));

        Set<UUID> parentIds =
                results.stream()
                        .map(Message::getReplyToId)
                        .filter(Objects::nonNull)
                        .collect(Collectors.toSet());
        Map<UUID, Message> parents =
                parentIds.isEmpty()
                        ? Map.of()
                        : messageRepository.findAllById(parentIds).stream()
                                .collect(Collectors.toMap(Message::getId, Function.identity()));

        List<MessageResponse> mapped =
                MessageMapper.toMessageResponseList(results, senders, parents);

        String nextCursor = null;

        if (hasMore) {
            Message last = results.getLast();
            nextCursor = new MessageCursor(last.getSentAt(), last.getId()).encode();
        }

        return new MessageHistoryResponse(mapped, nextCursor, hasMore);
    }

    @Transactional
    public MessageResponse editMessage(
            UUID roomId, UUID messageId, UUID callerId, UpdateMessageRequest request) {

        if (!roomMemberRepository.existsByRoomIdAndUserId(roomId, callerId)) {
            throw new RoomNotFoundException("Room not found: " + roomId);
        }

        Message message =
                messageRepository
                        .findByIdAndRoomId(messageId, roomId)
                        .orElseThrow(
                                () ->
                                        new MessageNotFoundException(
                                                "Message not found: " + messageId));

        if (!message.getSender().getId().equals(callerId)) {
            throw new RoomAccessDeniedException("Only sender can edit message");
        }

        message.setContent(request.content());
        message.setEditedAt(Instant.now(clock));

        MessageSender sender =
                new MessageSender(message.getSender().getId(), message.getSender().getUsername());

        Map<UUID, Message> parents = Map.of();

        if (message.getReplyToId() != null) {
            parents =
                    messageRepository
                            .findByIdAndRoomId(message.getReplyToId(), roomId)
                            .map(m -> Map.of(m.getId(), m))
                            .orElseGet(Map::of);
        }

        ReplyPreview preview = MessageMapper.resolveReplyPreview(message, parents);

        return MessageMapper.toMessageResponse(message, sender, preview);
    }

    @Transactional
    public List<UUID> deleteMessages(UUID roomId, UUID callerId, List<UUID> messageIds) {
        if (!roomMemberRepository.existsByRoomIdAndUserId(roomId, callerId)) {
            throw new RoomNotFoundException("Room not found: " + roomId);
        }

        List<Message> messages = messageRepository.findByRoomIdAndIdIn(roomId, messageIds);

        if (messages.size() != messageIds.size()) {
            throw new MessageNotFoundException("Some messages not found");
        }

        boolean isAdmin =
                roomMemberRepository.existsByRoomIdAndUserIdAndRole(
                        roomId, callerId, MemberRole.ADMIN);

        for (Message message : messages) {
            if (!(isAdmin || message.getSender().getId().equals(callerId))) {
                throw new RoomAccessDeniedException("Only admins or sender can delete messages");
            }

            message.setDeleted(true);
        }

        return messageIds;
    }
}
