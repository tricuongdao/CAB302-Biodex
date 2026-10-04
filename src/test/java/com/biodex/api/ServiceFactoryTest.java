package com.biodex.api;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * Pages share one recognition service, exactly as they share one species service.
 */
class ServiceFactoryTest {

    @Test
    void recognitionServiceIsASharedSingleton() {
        assertSame(ServiceFactory.recognitionService(), ServiceFactory.recognitionService());
    }
}