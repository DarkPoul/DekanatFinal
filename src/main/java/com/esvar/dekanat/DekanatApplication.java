package com.esvar.dekanat;

import com.esvar.dekanat.view.MainLayout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class DekanatApplication {
	private static final Logger log = LoggerFactory.getLogger(DekanatApplication.class);
	public static void main(String[] args) {
		SpringApplication.run(DekanatApplication.class, args);
		log.info("version alfa.0.0.2");
	}

}
