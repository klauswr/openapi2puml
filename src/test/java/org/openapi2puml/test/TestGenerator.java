package org.openapi2puml.test;

import java.io.File;
import java.io.IOException;

import org.junit.jupiter.api.Test;
import org.openapi2puml.openapi.plantuml.PlantUMLCodegen;
import org.springframework.util.Assert;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Paths;
import io.swagger.v3.parser.OpenAPIV3Parser;
import lombok.extern.log4j.Log4j2;

@Log4j2
public class TestGenerator {

	@Test
	public void testGenerate() throws IllegalAccessException, IOException {
		generate("src/test/resources/swagger.yaml");
	}

	@Test
	public void testGenerateItuCarrier4Platform() throws IllegalAccessException, IOException {
		generate("src/test/resources/ItuCarrier4Platform.yaml");
	}
	
	private void generate(String specFile) throws IllegalAccessException, IOException {
		log.info("Processing Swagger Spec File: " + specFile);
		File swaggerSpecFile = new File(specFile);
		Assert.isTrue(swaggerSpecFile.exists() && !swaggerSpecFile.isDirectory() && swaggerSpecFile.canRead(),
				"File " + swaggerSpecFile + " is invalid");
		OpenAPI swaggerObject = new OpenAPIV3Parser().read(swaggerSpecFile.getAbsolutePath());
		Paths paths = swaggerObject.getPaths();
		Components definitions = swaggerObject.getComponents();
		log.info("Swagger processing done");
		
		File targetLocation = new File("tmp");
		boolean generateDefinitionModelOnly = true;
		boolean includeCardinality = true;
		Assert.isTrue(targetLocation.exists() && targetLocation.isDirectory() && targetLocation.canRead(),
				"target location " + swaggerSpecFile + " is invalid");
        PlantUMLCodegen codegen = new PlantUMLCodegen(swaggerObject, targetLocation, generateDefinitionModelOnly,
                includeCardinality);
        String pumlPath = codegen.generatePlantUmlFile(swaggerObject);
        log.info("Successfully Created Plant UML output file {}", pumlPath);
	}
	
	
}
