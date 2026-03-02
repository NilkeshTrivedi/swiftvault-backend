package com.swiftvault.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling  // FIX #1: Required for @Scheduled jobs (FD maturity, RD auto-debit) to actually run
public class SwiftvaultBackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(SwiftvaultBackendApplication.class, args);
	}

}