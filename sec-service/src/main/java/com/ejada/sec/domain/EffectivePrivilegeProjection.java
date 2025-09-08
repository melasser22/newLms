package com.ejada.sec.domain;

public interface EffectivePrivilegeProjection {
    Long   getUserId();
    String getCode();
    String getResource();
    String getAction();
    Boolean getIsEffective();
}
