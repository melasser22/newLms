package com.ejada.sec.domain;

import jakarta.persistence.Embeddable;
import lombok.*;

import java.io.Serializable;

@Embeddable
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @EqualsAndHashCode
public class UserPrivilegeId implements Serializable {
    private static final long serialVersionUID = 1L;
    private Long userId;
    private Long privilegeId;
}
