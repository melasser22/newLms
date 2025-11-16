@edu.umd.cs.findbugs.annotations.SuppressFBWarnings(
    value = {"EI_EXPOSE_REP", "EI_EXPOSE_REP2"},
    justification = "Messaging models mirror external payload structures and allow mutable data")
package com.ejada.template.messaging.model;
