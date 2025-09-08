package com.ejada.sec.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "role_privileges")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class RolePrivilege {

    @EmbeddedId
    private RolePrivilegeId id;

    @ManyToOne(fetch = FetchType.LAZY) @MapsId("roleId")
    @JoinColumn(name = "role_id", nullable = false)
    private Role role;

    @ManyToOne(fetch = FetchType.LAZY) @MapsId("privilegeId")
    @JoinColumn(name = "privilege_id", nullable = false)
    private Privilege privilege;

    @Column(name = "granted_at", nullable = false)
    private Instant grantedAt = Instant.now();

    @Column(name = "granted_by")
    private Long grantedBy; // optional
}
