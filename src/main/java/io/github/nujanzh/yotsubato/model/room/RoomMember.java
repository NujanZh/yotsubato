package io.github.nujanzh.yotsubato.model.room;

import io.github.nujanzh.yotsubato.model.user.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Generated;
import org.hibernate.generator.EventType;
import org.hibernate.proxy.HibernateProxy;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@ToString
@Table(
        name = "room_members",
        uniqueConstraints =
                @UniqueConstraint(
                        name = "uniq_room_members_room_id_user_id",
                        columnNames = {"room_id", "user_id"}))
public class RoomMember {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "room_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_room_members_room_id"))
    @ToString.Exclude
    private Room room;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "user_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_room_members_user_id"))
    @ToString.Exclude
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MemberRole role;

    @Column(name = "joined_at", insertable = false, updatable = false)
    @Generated(event = {EventType.INSERT})
    private Instant joinedAt;

    public static RoomMember of(Room room, User user, MemberRole role) {
        RoomMember roomMember = new RoomMember();
        roomMember.setRoom(room);
        roomMember.setUser(user);
        roomMember.setRole(role);
        return roomMember;
    }

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
        RoomMember roomMember = (RoomMember) o;
        return getId() != null && Objects.equals(getId(), roomMember.getId());
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
