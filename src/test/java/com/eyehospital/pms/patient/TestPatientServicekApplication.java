package com.eyehospital.pms.patient;

import org.springframework.boot.SpringApplication;

public class TestPatientServicekApplication {

	public static void main(String[] args) {
		SpringApplication.from(PatientServicekApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
