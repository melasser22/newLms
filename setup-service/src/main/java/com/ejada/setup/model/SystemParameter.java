package com.ejada.setup.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@JsonIgnoreProperties(ignoreUnknown = true)
@Entity
@Table(name = "system_parameter")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class SystemParameter {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "param_id", unique = true, nullable = false)
    @EqualsAndHashCode.Include
    private Integer paramId;

    private static final int KEY_LENGTH = 150;
    private static final int VALUE_LENGTH = 1000;

    @Column(name = "param_key", nullable = false, unique = true, length = KEY_LENGTH)
    private String paramKey;

    @Column(name = "param_value", nullable = false, length = VALUE_LENGTH)
    private String paramValue;

    @Column(name = "description", length = VALUE_LENGTH)
    private String description;

    // DB column might be 'group_code' — map it to Java property 'paramGroup' that your service uses
    @Column(name = "group_code", length = KEY_LENGTH)
    private String paramGroup;

    @Builder.Default
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = Boolean.TRUE;

    public final Boolean isActive() {
        return isActive != null ? isActive : Boolean.FALSE;
    }

    public final String getGroupCode() {
        return paramGroup;
    }

    public final void setGroupCode(final String groupCode) {
        this.paramGroup = groupCode;
    }
}
