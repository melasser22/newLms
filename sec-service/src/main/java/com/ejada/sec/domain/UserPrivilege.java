package com.ejada.sec.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "user_privileges")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class UserPrivilege {

    @EmbeddedId
    private UserPrivilegeId id;

    @ManyToOne(fetch = FetchType.LAZY) @MapsId("userId")
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY) @MapsId("privilegeId")
    @JoinColumn(name = "privilege_id", nullable = false)
    private Privilege privilege;

    @Column(name = "is_granted", nullable = false)
    private boolean granted;

    @Column(name = "noted_at", nullable = false)
    @Builder.Default
    private Instant notedAt = Instant.now();

    @Column(name = "noted_by")
    private Long notedBy;
}
