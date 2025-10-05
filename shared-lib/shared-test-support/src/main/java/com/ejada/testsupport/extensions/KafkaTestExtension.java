package com.ejada.testsupport.extensions;

import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.HostConfig;
import com.github.dockerjava.api.model.Ports;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.opentest4j.TestAbortedException;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.utility.DockerImageName;

import java.util.LinkedHashMap;
import java.util.Map;

public class KafkaTestExtension implements BeforeAllCallback, AfterAllCallback {

    private static final ExtensionContext.Namespace NAMESPACE =
            ExtensionContext.Namespace.create(KafkaTestExtension.class);

    private static final DockerImageName KAFKA_IMAGE = DockerImageName.parse("apache/kafka:3.7.0");
    private static final String BOOTSTRAP_SERVERS = "localhost:29092";

    @SuppressWarnings("resource")
    private static final KafkaContainer KAFKA = new KafkaContainer(KAFKA_IMAGE)
            .withCreateContainerCmdModifier(cmd -> {
                HostConfig hostConfig = cmd.getHostConfig();
                if (hostConfig == null) {
                    hostConfig = new HostConfig();
                    cmd.withHostConfig(hostConfig);
                }
                Ports portBindings = hostConfig.getPortBindings();
                if (portBindings == null) {
                    portBindings = new Ports();
                }
                portBindings.bind(ExposedPort.tcp(9092), Ports.Binding.bindPort(29092));
                hostConfig.withPortBindings(portBindings);
            });

    @Override
    public void beforeAll(ExtensionContext context) {
        ensureDockerAvailable();
        ensureContainerStarted(context);

        Map<String, String> properties = new LinkedHashMap<>();
        properties.put("spring.kafka.bootstrap-servers", BOOTSTRAP_SERVERS);
        properties.put("shared.kafka.bootstrap-servers", BOOTSTRAP_SERVERS);

        Map<String, String> previousValues = SystemPropertyExtensionSupport.apply(properties);
        getClassStore(context).put(PropertiesHolder.KEY, new PropertiesHolder(previousValues));
    }

    @Override
    public void afterAll(ExtensionContext context) {
        PropertiesHolder holder = getClassStore(context).remove(PropertiesHolder.KEY, PropertiesHolder.class);
        if (holder != null) {
            SystemPropertyExtensionSupport.restore(holder.previousValues());
        }
    }

    private void ensureContainerStarted(ExtensionContext context) {
        ExtensionContext.Store rootStore = context.getRoot().getStore(NAMESPACE);
        rootStore.getOrComputeIfAbsent("kafka", key -> {
            if (!KAFKA.isRunning()) {
                KAFKA.start();
                verifyFixedBootstrapPort();
            }
            return (ExtensionContext.Store.CloseableResource) KAFKA::stop;
        });
    }

    private void verifyFixedBootstrapPort() {
        Integer mappedPort = KAFKA.getMappedPort(9092);
        if (mappedPort == null || mappedPort != 29092) {
            throw new IllegalStateException(
                    "Kafka container did not expose port 29092; actual mapped port was " + mappedPort);
        }
    }

    private ExtensionContext.Store getClassStore(ExtensionContext context) {
        return context.getStore(ExtensionContext.Namespace.create(KafkaTestExtension.class, context.getRequiredTestClass()));
    }

    private void ensureDockerAvailable() {
        try {
            DockerClientFactory.instance().client();
        } catch (IllegalStateException ex) {
            throw new TestAbortedException("Docker is not available for KafkaTestExtension", ex);
        }
    }

    public static boolean isRunning() {
        return KAFKA.isRunning();
    }

    public static String getBootstrapServers() {
        return BOOTSTRAP_SERVERS;
    }

    private record PropertiesHolder(Map<String, String> previousValues) {
        private static final String KEY = "kafka-properties";
    }
}
