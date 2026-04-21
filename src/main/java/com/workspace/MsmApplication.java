package com.workspace;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
public class MsmApplication {

	public static void main(String[] args) {
		SpringApplication.run(MsmApplication.class, args);
	}

}
