package com.ejada.tenant.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class TenantDto {
    private Long id;
    private String code;
    private String name;
    private String description;
}
