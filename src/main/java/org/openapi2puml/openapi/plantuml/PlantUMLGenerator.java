package org.openapi2puml.openapi.plantuml;

import io.swagger.models.Swagger;
import io.swagger.parser.SwaggerParser;
import net.sourceforge.plantuml.FileFormat;
import net.sourceforge.plantuml.FileFormatOption;
import net.sourceforge.plantuml.SourceFileReader;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import java.io.File;

public class PlantUMLGenerator {
  private static final Logger LOGGER = LogManager.getLogger(PlantUMLGenerator.class);

  public PlantUMLGenerator() {
    super();
  }

  public void transformOpenApi2Puml(String specFile, String output, boolean generateDefinitionModelOnly,
                                    boolean includeCardinality, boolean generateSvg, boolean generatePng) {
    File swaggerSpecFile = new File(specFile);
    File targetLocation = new File(output);


    if(!swaggerSpecFile.exists() ) {
    	throw new RuntimeException("Spec File " + swaggerSpecFile.getAbsolutePath() + " does not exist");
    }
    
    if(swaggerSpecFile.isDirectory()) {
    	throw new RuntimeException("Spec File " + swaggerSpecFile.getAbsolutePath() + " is not a file, but a directory");
    }
    
    if(!targetLocation.exists()) {
    	throw new RuntimeException("Output Location " + targetLocation.getAbsolutePath() + " does not exist");
    }

    if(!targetLocation.isDirectory()) {
    	throw new RuntimeException("Output Location " + targetLocation.getAbsolutePath() + " is not a directory");
    }

      LOGGER.info("Processing Swagger Spec File: " + specFile);
      Swagger swaggerObject = new SwaggerParser().read(swaggerSpecFile.getAbsolutePath());
      try {
        PlantUMLCodegen codegen = new PlantUMLCodegen(swaggerObject, targetLocation, generateDefinitionModelOnly,
            includeCardinality);
        String pumlPath = codegen.generatePlantUmlFile(swaggerObject);
        LOGGER.info("Successfully Created Plant UML output file!");

        if (generateSvg) {
          generateUmlDiagramFile(pumlPath, FileFormat.SVG);
        }
        if (generatePng) {
          generateUmlDiagramFile(pumlPath, FileFormat.PNG);
        }
      } catch (Exception e) {
        // TODO - Replace with better error message
        LOGGER.error(e.getMessage(), e);
        throw new RuntimeException(e);
      }
  }

  private void generateUmlDiagramFile(String plantUmlFilePath, FileFormat format) throws Exception {
    File pumlFile = new File(plantUmlFilePath);
    SourceFileReader sourceFileReader = new SourceFileReader(pumlFile, pumlFile.getParentFile());
    sourceFileReader.setFileFormatOption(new FileFormatOption(format));
    sourceFileReader.getGeneratedImages();
  }
}
