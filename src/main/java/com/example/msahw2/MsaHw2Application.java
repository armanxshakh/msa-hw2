package com.example.msahw2;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.EventListener;

@SpringBootApplication
public class MsaHw2Application {

	private static final Logger log =
			LoggerFactory.getLogger(MsaHw2Application.class);

	public static void main(String[] args) {
		SpringApplication.run(MsaHw2Application.class, args);
	}

	@EventListener(ApplicationReadyEvent.class)
	public void onApplicationReady() {
		log.atInfo()
				.addKeyValue("service", "msa-hw6")
				.log("Application started");
	}

	@EventListener(ContextClosedEvent.class)
	public void onApplicationStopped() {
		log.atInfo()
				.addKeyValue("service", "msa-hw6")
				.log("Application stopped");
	}
}