package com.ejada.subscription.acl.service;

import java.util.function.Supplier;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

@Slf4j
@Component
@RequiredArgsConstructor
public class NewTransactionExecutor {

    private final PlatformTransactionManager transactionManager;

    public <T> T execute(Supplier<T> supplier, String description) {
        TransactionTemplate template = new TransactionTemplate(transactionManager);
        template.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        try {
            return template.execute(status -> supplier.get());
        } catch (RuntimeException txEx) {
            log.error("{}", description, txEx);
            throw txEx;
        }
    }

    public void run(Runnable task, String description) {
        try {
            execute(
                    () -> {
                        task.run();
                        return null;
                    },
                    description);
        } catch (RuntimeException ignored) {
            // already logged inside execute
        }
    }
}
