package com.ejada.setup.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
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
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class SystemParameter {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "param_id", unique = true, nullable = false)
    @EqualsAndHashCode.Include
    private Integer paramId;

    @Column(name = "param_key", nullable = false, unique = true, length = 150)
    private String paramKey;

    @Column(name = "param_value", nullable = false, length = 1000)
    private String paramValue;

    @Column(name = "description", length = 1000)
    private String description;

    // DB column might be 'group_code' — map it to Java property 'paramGroup' that your service uses
    @Column(name = "group_code", length = 150)
    private String paramGroup;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = Boolean.TRUE;

    public Boolean isActive() { return isActive != null ? isActive : Boolean.FALSE; }

   public String getGroupCode() { return paramGroup; }
   public void setGroupCode(String groupCode) { this.paramGroup = groupCode; }
}
