package com.example.pre_view;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
public class PreViewApplication {

	public static void main(String[] args) {
		SpringApplication.run(PreViewApplication.class, args);
	}

}
