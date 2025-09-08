package com.ejada.sec.domain;

import jakarta.persistence.Embeddable;
import lombok.*;

import java.io.Serializable;

@Embeddable
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @EqualsAndHashCode
public class RolePrivilegeId implements Serializable {
    private Long roleId;
    private Long privilegeId;
}
