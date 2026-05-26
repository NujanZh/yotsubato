package io.github.nujanzh.yotsubato.model.message;

import io.github.nujanzh.yotsubato.model.room.Room;
import io.github.nujanzh.yotsubato.model.user.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.proxy.HibernateProxy;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@ToString
@Table(
        name = "messages",
        indexes = @Index(name = "idx_messages_room_sent", columnList = "room_id, sent_at"))
@SQLRestriction("deleted = false")
public class Message {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "sender_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_messages_sender_id"))
    @ToString.Exclude
    private User sender;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "room_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_messages_room_id"))
    @ToString.Exclude
    private Room room;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reply_to", foreignKey = @ForeignKey(name = "fk_messages_reply_to"))
    @ToString.Exclude
    private Message replyTo;

    @OneToMany(mappedBy = "replyTo", fetch = FetchType.LAZY)
    @ToString.Exclude
    private List<Message> replies = new ArrayList<>();

    @OneToMany(mappedBy = "message", fetch = FetchType.LAZY)
    @ToString.Exclude
    private List<MessageStatus> statuses = new ArrayList<>();

    @OneToMany(mappedBy = "message", fetch = FetchType.LAZY)
    @ToString.Exclude
    private List<Attachment> attachments = new ArrayList<>();

    @Column(columnDefinition = "TEXT")
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MessageType type;

    @Column(name = "edited_at")
    private Instant editedAt;

    private boolean deleted;

    @Column(name = "sent_at", insertable = false, updatable = false)
    private Instant sentAt;

    @Override
    public final boolean equals(Object o) {
        if (this == o) return true;
        if (o == null) return false;
        Class<?> oEffectiveClass =
                o instanceof HibernateProxy
                        ? ((HibernateProxy) o).getHibernateLazyInitializer().getPersistentClass()
                        : o.getClass();
        Class<?> thisEffectiveClass =
                this instanceof HibernateProxy
                        ? ((HibernateProxy) this).getHibernateLazyInitializer().getPersistentClass()
                        : this.getClass();
        if (thisEffectiveClass != oEffectiveClass) return false;
        Message message = (Message) o;
        return getId() != null && Objects.equals(getId(), message.getId());
    }

    @Override
    public final int hashCode() {
        return this instanceof HibernateProxy
                ? ((HibernateProxy) this)
                        .getHibernateLazyInitializer()
                        .getPersistentClass()
                        .hashCode()
                : getClass().hashCode();
    }
}
