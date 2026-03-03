package com.eyehospital.pms;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import jakarta.transaction.Transactional;

import org.junit.jupiter.api.BeforeEach;

/**
 * Base class for all integration tests.
 *
 * <p>Provides a fully configured Spring application context backed by a
 * PostgreSQL Testcontainer. Subclasses inherit all common annotations
 * and the pre‑configured {@link MockMvc} instance.</p>
 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
@ActiveProfiles("test")
@Transactional
public abstract class BaseIntegrationTest {

    @Autowired
    private WebApplicationContext wac;

    protected MockMvc mockMvc;

    @BeforeEach
    void setUpMockMvc() {
        mockMvc = MockMvcBuilders.webAppContextSetup(wac).build();
    }
}