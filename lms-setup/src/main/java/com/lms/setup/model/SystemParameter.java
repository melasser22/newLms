package com.lms.setup.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;

@JsonIgnoreProperties(ignoreUnknown = true)
@Entity
@Table(name = "system_parameter")
public class SystemParameter {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "param_id", unique = true, nullable = false)
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

    // --- getters & setters ---
    public Integer getParamId() { return paramId; }
    public void setParamId(Integer paramId) { this.paramId = paramId; }

    public String getParamKey() { return paramKey; }
    public void setParamKey(String paramKey) { this.paramKey = paramKey; }

    public String getParamValue() { return paramValue; }
    public void setParamValue(String paramValue) { this.paramValue = paramValue; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getParamGroup() { return paramGroup; }
    public void setParamGroup(String paramGroup) { this.paramGroup = paramGroup; }

    public Boolean getIsActive() { return isActive; }
    public Boolean isActive() { return isActive != null ? isActive : Boolean.FALSE; }
    public void setIsActive(Boolean active) { isActive = active; }

   public String getGroupCode() { return paramGroup; }
   public void setGroupCode(String groupCode) { this.paramGroup = groupCode; }
}
