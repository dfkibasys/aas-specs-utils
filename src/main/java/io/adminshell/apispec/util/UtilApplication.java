package io.adminshell.apispec.util;

import io.adminshell.apispec.util.service.OpenApiProcessor;
import io.adminshell.apispec.util.service.WordGenerator;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;

import java.io.IOException;

@SpringBootApplication
@Slf4j
public class UtilApplication implements CommandLineRunner {

	@Autowired
	private OpenApiProcessor processor;

	@Autowired
	private WordGenerator wordGenerator;

	private static Logger LOG = LoggerFactory
			.getLogger(UtilApplication.class);



	public static void main(String[] args) {
		SpringApplication app = new SpringApplicationBuilder()
				.sources(UtilApplication.class)
				.web(WebApplicationType.NONE).build();

		app.run(args).close();

		//SpringApplication.run(UtilApplication.class, args).close();
	}

	@Override
	public void run(String... args) {

		for (int i = 0; i < args.length; ++i) {
			LOG.info("args[{}]: {}", i, args[i]);
		}

		String location = "C:/Repos/aas-specs-api/AssetAdministrationShellRepositoryServiceSpecification/V3.0.yaml";
		String folder = "C:/Repos/aas-specs-api/";
		try {
			processor.processFolder();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		//processor.processFile(location);
		//wordGenerator.generateWord();

	}

}
