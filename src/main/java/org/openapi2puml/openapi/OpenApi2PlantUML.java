package org.openapi2puml.openapi;

import org.openapi2puml.openapi.plantuml.PlantUMLGenerator;
public class OpenApi2PlantUML {

  	/**
	 * @param specFile
	 * @param output
	 * @param generateDefinitionModelOnly
	 * @param includeCardinality
	 * @param generateSvg
	 * @param generatePng
	 */
  public static void process(String specFile, String output, boolean generateDefinitionModelOnly, boolean includeCardinality,
                       boolean generateSvg, boolean generatePng) {
    PlantUMLGenerator generator = new PlantUMLGenerator();
    generator.transformOpenApi2Puml(specFile, output, generateDefinitionModelOnly, includeCardinality, generateSvg, generatePng);
  }
}
