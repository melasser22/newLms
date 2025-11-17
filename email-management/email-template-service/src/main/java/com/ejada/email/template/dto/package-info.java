@edu.umd.cs.findbugs.annotations.SuppressFBWarnings(
    value = {"EI_EXPOSE_REP", "EI_EXPOSE_REP2"},
    justification = "DTO classes intentionally expose mutable fields for serialization and validation frameworks")
package com.ejada.email.template.dto;
