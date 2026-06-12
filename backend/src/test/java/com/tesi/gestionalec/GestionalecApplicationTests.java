package com.tesi.gestionalec;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Test di integrazione: richiede MySQL attivo e configurato in application.properties.
 * Eseguire manualmente con il DB avviato — non fa parte della suite CI unitaria.
 */
@SpringBootTest
@Disabled("Test di integrazione: richiede MySQL attivo")
class GestionalecApplicationTests {

    @Test
    void contextLoads() {
    }

}
