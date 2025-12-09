package com.example.be;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.stream.Stream;

@SpringBootApplication
public class BeApplication {

	public static void main(String[] args) {
		loadEnv();
		SpringApplication.run(BeApplication.class, args);
	}

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }


    private static void loadEnv() {
		try {
			// Try to load .env from current directory
			if (Files.exists(Paths.get(".env"))) {
				try (Stream<String> lines = Files.lines(Paths.get(".env"))) {
					lines.filter(line -> line.contains("=") && !line.startsWith("#"))
							.forEach(line -> {
								String[] parts = line.split("=", 2);
								if (parts.length == 2) {
									System.setProperty(parts[0].trim(), parts[1].trim());
								}
							});
				}
				System.out.println("Loaded environment variables from .env file");
			} else {
				System.out.println(".env file not found in current directory");
			}
		} catch (IOException e) {
			System.err.println("Failed to load .env file: " + e.getMessage());
		}
	}
}
