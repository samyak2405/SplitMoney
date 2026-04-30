package com.javaproject.splitewise;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class SplitewiseBackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(SplitewiseBackendApplication.class, args);
	}

}
