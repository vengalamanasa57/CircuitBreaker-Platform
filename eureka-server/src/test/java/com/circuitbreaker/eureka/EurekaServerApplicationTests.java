package com.circuitbreaker.eureka;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class EurekaServerApplicationTests {

    @Test
    void contextLoads() {
        // Fails if the registry cannot start with the shipped configuration.
    }
}
