package org.openapi2puml.openapi.plantuml.helpers;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openapi2puml.openapi.plantuml.FormatUtility;
import org.openapi2puml.openapi.plantuml.vo.ClassRelation;
import org.openapi2puml.openapi.plantuml.vo.InterfaceDiagram;
import org.openapi2puml.openapi.plantuml.vo.MethodDefinitions;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.Paths;
import io.swagger.v3.oas.models.media.ArraySchema;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.parameters.PathParameter;
import io.swagger.v3.oas.models.parameters.QueryParameter;
import io.swagger.v3.oas.models.parameters.RequestBody;
import io.swagger.v3.oas.models.responses.ApiResponse;

public class PlantUMLInterfaceDiagramHelper {
    private static final String APPLICATION_JSON = "application/json";

    private static final Logger logger = LogManager.getLogger(PlantUMLInterfaceDiagramHelper.class);

    private static final String SUFFIX_API = "Api";

    public List<InterfaceDiagram> processSwaggerPaths(OpenAPI swagger) {
        Map<String, InterfaceDiagram> interfaceDiagramMap = new HashMap<>();
//		Map<String, Path> paths = swagger.getPaths();
        Paths paths = swagger.getPaths();

        logger.debug("Swagger Paths to Process to PlantUML Interfaces: " + paths.keySet().toString());

        for (Entry<String, PathItem> entry : paths.entrySet()) {
            PathItem pathObject = entry.getValue();

            logger.debug("Processing Path: " + entry.getKey());

            List<Operation> operations = pathObject.readOperations();
            String uri = entry.getKey();

            for (Operation operation : operations) {
                // TODO - refactor to take the map of existing InterfaceDiagrams and check if
                // it's found instead of the merge
                InterfaceDiagram interfaceDiagram = getInterfaceDiagram(operation, uri);
                InterfaceDiagram existingInterfaceDiagram = interfaceDiagramMap
                        .get(interfaceDiagram.getInterfaceName());
                if (existingInterfaceDiagram == null) {
                    interfaceDiagramMap.put(interfaceDiagram.getInterfaceName(), interfaceDiagram);
                } else {
                    mergeInterfaceDiagrams(existingInterfaceDiagram, interfaceDiagram);
                }
            }
        }

        return new ArrayList<InterfaceDiagram>(interfaceDiagramMap.values());
    }

    private InterfaceDiagram mergeInterfaceDiagrams(InterfaceDiagram existingInterface, InterfaceDiagram newInterface) {

        if (existingInterface.getInterfaceName().equalsIgnoreCase(newInterface.getInterfaceName())) {
            // add any missing details of new to existing
            existingInterface
                    .setErrorClasses(Stream.of(existingInterface.getErrorClasses(), newInterface.getErrorClasses())
                            .flatMap(Collection::stream).distinct().collect(Collectors.toList()));

            existingInterface.setMethods(Stream.of(existingInterface.getMethods(), newInterface.getMethods())
                    .flatMap(Collection::stream).distinct().collect(Collectors.toList()));

            existingInterface
                    .setChildClasses(Stream.of(existingInterface.getChildClasses(), newInterface.getChildClasses())
                            .flatMap(Collection::stream).distinct().collect(Collectors.toList()));
        }

        return existingInterface;
    }

    private InterfaceDiagram getInterfaceDiagram(Operation operation, String uri) {
        InterfaceDiagram interfaceDiagram = new InterfaceDiagram();
        String interfaceName = getInterfaceName(operation.getTags(), operation, uri);
        List<String> errorClassNames = getErrorClassNames(operation);
        interfaceDiagram.setInterfaceName(interfaceName);
        interfaceDiagram.setErrorClasses(errorClassNames);
        interfaceDiagram.setMethods(getInterfaceMethods(operation));
        interfaceDiagram.setChildClasses(getInterfaceRelations(operation, errorClassNames));

        return interfaceDiagram;
    }

    private String getInterfaceName(List<String> tags, Operation operation, String uri) {
        String interfaceName;

        if (tags != null && !tags.isEmpty()) {
            interfaceName = FormatUtility.toTitleCase(tags.get(0).replaceAll(" ", ""));
        } else if (StringUtils.isNotEmpty(operation.getOperationId())) {
            interfaceName = FormatUtility.toTitleCase(operation.getOperationId());
        } else {
            interfaceName = FormatUtility.toTitleCase(uri.replaceAll("{", "").replaceAll("}", "").replaceAll("\\", ""));
        }

        return interfaceName + SUFFIX_API;
    }

    private List<String> getErrorClassNames(Operation operation) {
        List<String> errorClasses = new ArrayList<>();
        Map<String, ApiResponse> responses = operation.getResponses();

        for (Entry<String, ApiResponse> responsesEntry : responses.entrySet()) {
            String responseCode = responsesEntry.getKey();

            if (responseCode.equalsIgnoreCase("default") || Integer.parseInt(responseCode) >= 300) {
                MediaType mediaType = responsesEntry.getValue().getContent().get(APPLICATION_JSON);
                if (mediaType == null) {
                    continue;
                }
                Schema responseProperty = mediaType.getSchema();
                if (responseProperty instanceof ObjectSchema) {
                    String errorClassName = getSimpleRef(((ObjectSchema) responseProperty).get$ref());
                    if (!errorClasses.contains(errorClassName)) {
                        errorClasses.add(errorClassName);
                    }
                }
            }
        }

        return errorClasses;
    }

    private List<ClassRelation> getInterfaceRelatedInputs(Operation operation) {
        // TODO refactor to pass source class name
        List<ClassRelation> relatedResponses = new ArrayList<>();

        RequestBody requestBody = operation.getRequestBody();
        if (null != requestBody) {

            Schema bodyParameter = requestBody.getContent().get(APPLICATION_JSON).getSchema();
            if (bodyParameter instanceof ObjectSchema) {

                ClassRelation classRelation = new ClassRelation();
                classRelation.setTargetClass(getSimpleRef(((ObjectSchema) bodyParameter).get$ref()));
                classRelation.setComposition(false);
                classRelation.setExtension(true);

                relatedResponses.add(classRelation);
            } else if (bodyParameter instanceof ArraySchema) {
                Schema propertyObject = ((ArraySchema) bodyParameter).getItems();

                if (propertyObject instanceof ObjectSchema) {
                    ClassRelation classRelation = new ClassRelation();
                    classRelation.setTargetClass(getSimpleRef(((ObjectSchema) propertyObject).get$ref()));
                    classRelation.setComposition(false);
                    classRelation.setExtension(true);

                    relatedResponses.add(classRelation);
                }
            }
        }
        return relatedResponses;
    }

    private List<ClassRelation> getInterfaceRelatedResponses(Operation operation) {
        // TODO refactor to pass source class name
        List<ClassRelation> relatedResponses = new ArrayList<>();
        Map<String, ApiResponse> responses = operation.getResponses();

        for (Entry<String, ApiResponse> responsesEntry : responses.entrySet()) {
            String responseCode = responsesEntry.getKey();

            if (!(responseCode.equalsIgnoreCase("default") || Integer.parseInt(responseCode) >= 300)) {
                Schema responseProperty = responsesEntry.getValue().getContent().get(APPLICATION_JSON).getSchema();

                if (responseProperty instanceof ObjectSchema) {
                    ClassRelation relation = new ClassRelation();
                    relation.setTargetClass(getSimpleRef(((ObjectSchema) responseProperty).get$ref()));
                    relation.setComposition(false);
                    relation.setExtension(true);

                    relatedResponses.add(relation);
                } else if (responseProperty instanceof ArraySchema) {
                    ArraySchema arrayObject = (ArraySchema) responseProperty;
                    Schema arrayResponseProperty = arrayObject.getItems();

                    if (arrayResponseProperty instanceof ObjectSchema) {
                        ClassRelation relation = new ClassRelation();
                        relation.setTargetClass(getSimpleRef(((ObjectSchema) arrayResponseProperty).get$ref()));
                        relation.setComposition(false);
                        relation.setExtension(true);
                        relatedResponses.add(relation);
                    }
                }
            }

        }

        return relatedResponses;
    }

    private List<MethodDefinitions> getInterfaceMethods(Operation operation) {
        List<MethodDefinitions> interfaceMethods = new ArrayList<>();
        MethodDefinitions methodDefinitions = new MethodDefinitions();
        methodDefinitions.setMethodDefinition(operation.getOperationId() + "(" + getMethodParameters(operation) + ")");
        methodDefinitions.setReturnType(getInterfaceReturnType(operation));

        interfaceMethods.add(methodDefinitions);

        return interfaceMethods;
    }

    private String getMethodParameters(Operation operation) {
        String methodParameter = "";
        List<Parameter> parameters = operation.getParameters();

        RequestBody requestBody = operation.getRequestBody();
        if (null != requestBody) {
            Schema bodyParameter = requestBody.getContent().get(APPLICATION_JSON).getSchema();
            if (bodyParameter instanceof ObjectSchema) {
                methodParameter += FormatUtility.toTitleCase(getSimpleRef(((ObjectSchema) bodyParameter).get$ref()))
                        + ((ObjectSchema) bodyParameter).getName();
            } else if (bodyParameter instanceof ArraySchema) {
                Schema propertyObject = ((ArraySchema) bodyParameter).getItems();

                if (propertyObject instanceof ObjectSchema) {
                    methodParameter += FormatUtility
                            .toTitleCase(getSimpleRef(((ObjectSchema) propertyObject).get$ref())) + "[] "
                            + ((ObjectSchema) bodyParameter).getName();
                }
            }
        }

        if (parameters != null) {
            for (Parameter parameter : parameters) {
                if (StringUtils.isNotEmpty(methodParameter)) {
                    methodParameter += ",";
                }

                if (parameter instanceof PathParameter) {
                    methodParameter += FormatUtility.toTitleCase(((PathParameter) parameter).getSchema().getType())
                            + " " + ((PathParameter) parameter).getName();
                } else if (parameter instanceof QueryParameter) {
                    Schema queryParameterProperty = ((QueryParameter) parameter).getSchema();

                    if (queryParameterProperty instanceof ObjectSchema) {
                        methodParameter += FormatUtility
                                .toTitleCase(getSimpleRef(((ObjectSchema) queryParameterProperty).get$ref())) + "[] "
                                + parameter.getName();
                    } else if (queryParameterProperty instanceof StringSchema) {
                        methodParameter += FormatUtility.toTitleCase(queryParameterProperty.getType()) + "[] "
                                + parameter.getName();
                    } else {
                        methodParameter += FormatUtility
                                .toTitleCase(((QueryParameter) parameter).getContent().keySet().toString() + " "
                                        + ((QueryParameter) parameter).getName());
                    }
                }
// TODO		} else if (parameter instanceof FormParameter) {
//				methodParameter += FormatUtility.toTitleCase(((FormParameter) parameter).getType()) + " "
//						+ ((FormParameter) parameter).getName();
            }
        }
        return methodParameter;
    }

    private String getInterfaceReturnType(Operation operation) {
        String returnType = "void";

        Map<String, ApiResponse> responses = operation.getResponses();
        for (Entry<String, ApiResponse> responsesEntry : responses.entrySet()) {
            String responseCode = responsesEntry.getKey();

            if (!(responseCode.equalsIgnoreCase("default") || Integer.parseInt(responseCode) >= 300)) {
                Schema responseProperty = responsesEntry.getValue().getContent().get(APPLICATION_JSON).getSchema();

                if (responseProperty instanceof ObjectSchema) {
                    returnType = getSimpleRef(((ObjectSchema) responseProperty).get$ref());
                } else if (responseProperty instanceof ArraySchema) {
                    Schema arrayResponseProperty = ((ArraySchema) responseProperty).getItems();
                    if (arrayResponseProperty instanceof ObjectSchema) {
                        returnType = getSimpleRef(((ObjectSchema) arrayResponseProperty).get$ref()) + "[]";
                    }
                } else if (responseProperty instanceof ObjectSchema) {
                    returnType = FormatUtility.toTitleCase(operation.getOperationId()) + "Generated";
                }
            }
        }

        return returnType;
    }

    private String getSimpleRef(String fullRef) {
        return StringUtils.substringAfter(fullRef, Components.COMPONENTS_SCHEMAS_REF);
    }

    private List<ClassRelation> getInterfaceRelations(Operation operation, List<String> errorClassNames) {
        List<ClassRelation> relations = new ArrayList<>();
        relations.addAll(getInterfaceRelatedResponses(operation));
        relations.addAll(getInterfaceRelatedInputs(operation));
        for (String errorClassName : errorClassNames) {
            relations.add(getErrorClass(errorClassName));
        }

        return filterUnique(relations, true);
    }

    private ClassRelation getErrorClass(String errorClassName) {
        ClassRelation classRelation = new ClassRelation();
        classRelation.setTargetClass(errorClassName);
        classRelation.setComposition(false);
        classRelation.setExtension(true);

        return classRelation;
    }

    private List<ClassRelation> filterUnique(List<ClassRelation> relations, boolean compareTargetOnly) {
        List<ClassRelation> uniqueList = new ArrayList<>();

        for (ClassRelation relation : relations) {
            if (!isTargetClassInMap(relation, uniqueList, compareTargetOnly)) {
                uniqueList.add(relation);
            }
        }

        return uniqueList;
    }

    private boolean isTargetClassInMap(ClassRelation sourceRelation, List<ClassRelation> relatedResponses,
            boolean considerTargetOnly) {
        for (ClassRelation relation : relatedResponses) {

            if (considerTargetOnly) {
                if (StringUtils.isNotEmpty(relation.getTargetClass())
                        && StringUtils.isNotEmpty(sourceRelation.getTargetClass())
                        && relation.getTargetClass().equalsIgnoreCase(sourceRelation.getTargetClass())) {
                    return true;
                }
            } else {
                if (StringUtils.isNotEmpty(relation.getSourceClass())
                        && StringUtils.isNotEmpty(sourceRelation.getSourceClass())
                        && StringUtils.isNotEmpty(relation.getTargetClass())
                        && StringUtils.isNotEmpty(sourceRelation.getTargetClass())
                        && relation.getSourceClass().equalsIgnoreCase(sourceRelation.getSourceClass())
                        && relation.getTargetClass().equalsIgnoreCase(sourceRelation.getTargetClass())) {

                    return true;
                }
            }
        }

        return false;
    }

}
