package com.eyehospital.pms.patient;

import org.springframework.boot.SpringApplication;
import org.springframework.test.context.ActiveProfiles;
@ActiveProfiles("test")
public class TestPatientServicekApplication {

	public static void main(String[] args) {
		SpringApplication.from(PatientServicekApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
