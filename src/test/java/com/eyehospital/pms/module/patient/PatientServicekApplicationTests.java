package com.eyehospital.pms.module.patient;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import com.eyehospital.pms.TestcontainersConfiguration;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@ActiveProfiles("test")
class PatientServicekApplicationTests {

	@Test
	void contextLoads() {
		assertTrue(true);
	}

}
