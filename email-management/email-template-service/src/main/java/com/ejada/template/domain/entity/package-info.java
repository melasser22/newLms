@edu.umd.cs.findbugs.annotations.SuppressFBWarnings(
    value = {"EI_EXPOSE_REP", "EI_EXPOSE_REP2"},
    justification = "JPA entities expose mutable collections for persistence frameworks")
package com.ejada.template.domain.entity;
