package com.jhklim.investsim;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@EnableJpaAuditing
@SpringBootApplication
public class InvestsimApplication {

	public static void main(String[] args) {
		SpringApplication.run(InvestsimApplication.class, args);
	}

}
