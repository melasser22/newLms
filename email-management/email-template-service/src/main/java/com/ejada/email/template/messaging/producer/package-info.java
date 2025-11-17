@edu.umd.cs.findbugs.annotations.SuppressFBWarnings(
    value = {"EI_EXPOSE_REP2"},
    justification = "Kafka producers rely on framework-managed templates injected by Spring")
package com.ejada.template.messaging.producer;
