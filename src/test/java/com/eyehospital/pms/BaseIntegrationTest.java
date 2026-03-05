package com.eyehospital.pms;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import jakarta.transaction.Transactional;

/**
 * Base class for all integration tests.
 *
 * <p>Provides a fully configured Spring application context backed by a
 * PostgreSQL Testcontainer. Subclasses inherit all common annotations
 * and the pre‑configured {@link MockMvc} instance.</p>
 *
 * <p>MockMvc is auto-configured with all filters enabled by default.
 * Subclasses can override this behaviour by re-declaring
 * {@code @AutoConfigureMockMvc(addFilters = false)}.</p>
 */
@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@Import(TestcontainersConfiguration.class)
@ActiveProfiles("test")
@Transactional
public abstract class BaseIntegrationTest {

    @Autowired
    protected MockMvc mockMvc;
}