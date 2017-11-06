package edu.sjsu.posturize.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class PosturizeApiApplication {

	public static void main(String[] args) {
		SpringApplication.run(PosturizeApiApplication.class, args);
	}
}

