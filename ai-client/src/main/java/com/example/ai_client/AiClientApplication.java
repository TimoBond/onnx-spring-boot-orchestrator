package com.example.ai_client;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;
@SpringBootApplication
@EnableScheduling
public class AiClientApplication {

	public static void main(String[] args) {
		SpringApplication.run(AiClientApplication.class, args);
	}


}
