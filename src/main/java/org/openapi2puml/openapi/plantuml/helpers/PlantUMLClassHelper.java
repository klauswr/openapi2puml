package org.openapi2puml.openapi.plantuml.helpers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.openapi2puml.openapi.plantuml.FormatUtility;
import org.openapi2puml.openapi.plantuml.vo.ClassDiagram;
import org.openapi2puml.openapi.plantuml.vo.ClassMembers;
import org.openapi2puml.openapi.plantuml.vo.ClassRelation;

import io.swagger.models.ArrayModel;
import io.swagger.models.ComposedModel;
import io.swagger.models.Model;
import io.swagger.models.ModelImpl;
import io.swagger.models.RefModel;
import io.swagger.models.Swagger;
import io.swagger.models.properties.ArrayProperty;
import io.swagger.models.properties.Property;
import io.swagger.models.properties.RefProperty;
import io.swagger.models.properties.StringProperty;
import io.swagger.v3.oas.models.media.ArraySchema;
import io.swagger.v3.oas.models.media.ComposedSchema;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import lombok.extern.log4j.Log4j2;

@Log4j2
public class PlantUMLClassHelper {

    private boolean includeCardinality;

    private static final String CARDINALITY_NONE_TO_ONE = "0..1";
    private static final String CARDINALITY_ONE_TO_ONE = "1..1";
    private static final String CARDINALITY_ONE_TO_MANY = "1..*";
    private static final String CARDINALITY_NONE_TO_MANY = "0..*";

    public PlantUMLClassHelper(boolean includeCardinality) {
        this.includeCardinality = includeCardinality;
    }

    public List<ClassDiagram> processSwaggerModels(OpenAPI swagger) {

        List<ClassDiagram> classDiagrams = new ArrayList<>();
        Map<String, Schema> schemaMap = swagger.getComponents().getSchemas();

        log.debug("Swagger Models to Process to PlantUML Classes: " + schemaMap.keySet().toString());

        for (Entry<String, Schema> models : schemaMap.entrySet()) {
            String className = models.getKey();
            Schema modelObject = models.getValue();

            log.debug("Processing Model: " + className);

            String superClass = getSuperClass(modelObject);
            List<ClassMembers> classMembers = getClassMembers(modelObject, schemaMap);

            classDiagrams.add(new ClassDiagram(className, modelObject.getDescription(), classMembers,
                    getChildClasses(classMembers, superClass), isSchemaClass(modelObject), superClass));
        }

        return classDiagrams;
    }

    private boolean isSchemaClass(Schema schema) {
        boolean isModelClass = true;

        if (schema instanceof Schema) {
            List<String> enumValues = ((Schema) schema).getEnum();

            if (enumValues != null && !enumValues.isEmpty()) {
                isModelClass = false;
            }
        }

        return isModelClass;
    }

    private String getSuperClass(Schema schemaObject) {
        String superClass = null;

        if (schemaObject instanceof ArraySchema) {
            ArraySchema arraySchema = (ArraySchema) schemaObject;
            Schema<?> items = arraySchema.getItems();

            if (items instanceof Schema) {
                superClass = "ArrayList[" + ((Schema) items).get$ref() + "]";
            }
        } else if (schemaObject instanceof Schema) {
            Object addProperty = ((Schema) schemaObject).getAdditionalProperties();

            if (addProperty instanceof Schema) {
                superClass = "Map[" + ((Schema) addProperty).get$ref() + "]";
            }
        }

        return superClass;
    }

    private List<ClassRelation> getChildClasses(List<ClassMembers> classMembers, String superClass) {
        List<ClassRelation> childClasses = new ArrayList<>();

        for (ClassMembers member : classMembers) {

            boolean alreadyExists = false;

            for (ClassRelation classRelation : childClasses) {

                if (classRelation.getTargetClass().equalsIgnoreCase(member.getClassName())) {
                    alreadyExists = true;
                }
            }

            // TODO - why do we not set the source class name here instead of in the
            // interface diagram handling
            if (!alreadyExists && member.getClassName() != null && member.getClassName().trim().length() > 0) {
                if (StringUtils.isNotEmpty(superClass)) {
                    childClasses
                            .add(new ClassRelation(member.getClassName(), true, false, member.getCardinality(), null));
                } else {
                    childClasses
                            .add(new ClassRelation(member.getClassName(), false, true, member.getCardinality(), null));
                }
            }
        }

        return childClasses;
    }

    private List<ClassMembers> getClassMembers(Schema<?> schemaObject, Map<String, Schema<?>> modelsMap) {
        List<ClassMembers> classMembers = new ArrayList<>();

        if (schemaObject instanceof ObjectSchema) {
            classMembers = getClassMembers((ObjectSchema) schemaObject, modelsMap);
        } else if (schemaObject instanceof ComposedSchema) {
            classMembers = getClassMembers((ComposedSchema) schemaObject, modelsMap);
        } else if (schemaObject instanceof ArraySchema) {
            classMembers = getClassMembers((ArraySchema) schemaObject, modelsMap);
        }

        return classMembers;
    }

    private List<ClassMembers> getClassMembers(ArraySchema arrayModel, Map<String, Model> modelsMap) {

        List<ClassMembers> classMembers = new ArrayList<>();

        Schema<?> propertyObject = arrayModel.getItems();

        if (propertyObject instanceof RefProperty) {
            classMembers.add(getRefClassMembers((RefProperty) propertyObject));
        }

        return classMembers;
    }

    private List<ClassMembers> getClassMembers(ComposedSchema composedModel, Map<String, Model> modelsMap) {
        return getClassMembers(composedModel, modelsMap, new HashSet<>());
    }

    /**
     * New Overloaded getClassMembers Implementation to handle deeply nested class
     * hierarchies
     * 
     * @param composedModel
     * @param modelsMap
     * @param visited
     * @return
     */
    private List<ClassMembers> getClassMembers(ComposedSchema composedModel, Map<String, Model> modelsMap,
            Set<Model> visited) {
        List<ClassMembers> classMembers = new ArrayList<>();
        Map<String, Schema> childProperties = new HashMap<>();

        if (null != composedModel.getProperties()) {
            childProperties = composedModel.getProperties();
        }

        List<ClassMembers> ancestorMembers;

        List<Schema> allOf = composedModel.getAllOf();
        for (Schema currentModel : allOf) {

            if (currentModel instanceof RefModel) {
                RefModel refModel = (RefModel) currentModel;
                // This line throws an NPE when encountering deeply nested class hierarchies
                // because it assumes any child
                // classes are RefModel and not ComposedModel
                // childProperties.putAll(modelsMap.get(refModel.getSimpleRef()).getProperties());

                Model parentRefModel = modelsMap.get(refModel.getSimpleRef());

                if (parentRefModel.getProperties() != null) {
                    childProperties.putAll(parentRefModel.getProperties());
                }

                classMembers = convertModelPropertiesToClassMembers(childProperties,
                        modelsMap.get(refModel.getSimpleRef()), modelsMap);

                // If the parent model also has AllOf references -- meaning it's a child of some
                // other superclass
                // then we need to recurse to get the grandparent's properties and add them to
                // our current classes
                // derived property list
                if (parentRefModel instanceof ComposedModel) {
                    ComposedModel parentRefComposedModel = (ComposedModel) parentRefModel;
                    // Use visited to mark which classes we've processed -- this is just to avoid
                    // an infinite loop in case there's a circular reference in the class hierarchy.
                    if (!visited.contains(parentRefComposedModel)) {
                        ancestorMembers = getClassMembers(parentRefComposedModel, modelsMap, visited);
                        classMembers.addAll(ancestorMembers);
                    }
                }
            }
        }

        visited.add(composedModel);
        return classMembers;
    }

    private List<ClassMembers> getClassMembers(ObjectSchema model, Map<String, Model> modelsMap) {
        List<ClassMembers> classMembers = new ArrayList<>();

        Map<String, Schema> modelMembers = model.getProperties();
        if (modelMembers != null && !modelMembers.isEmpty()) {
            classMembers.addAll(convertModelPropertiesToClassMembers(modelMembers, model, modelsMap));
        } else {
            Object modelAdditionalProps = model.getAdditionalProperties();

            if (modelAdditionalProps instanceof RefProperty) {
                classMembers.add(getRefClassMembers((RefProperty) modelAdditionalProps));
            }

            if (modelAdditionalProps == null) {
                List<Object> enumValues = model.getEnum();

                if (enumValues != null && !enumValues.isEmpty()) {
                    classMembers.addAll(getEnum(enumValues));
                }
            }
        }

        return classMembers;
    }

    private ClassMembers getRefClassMembers(RefProperty refProperty) {
        ClassMembers classMember = new ClassMembers();
        classMember.setClassName(refProperty.getSimpleRef());
        classMember.setName(" ");

        if (includeCardinality) {
            classMember.setCardinality(CARDINALITY_NONE_TO_MANY);
        }

        return classMember;
    }

    private List<ClassMembers> getEnum(List<Object> enumValues) {

        List<ClassMembers> classMembers = new ArrayList<>();

        if (enumValues != null && !enumValues.isEmpty()) {
            for (Object enumValue : enumValues) {
                ClassMembers classMember = new ClassMembers();
                // TODO??? really to String? or cast? 
                classMember.setName(enumValue.toString());
                classMembers.add(classMember);
            }
        }

        return classMembers;
    }

    private List<ClassMembers> convertModelPropertiesToClassMembers(Map<String, Schema> modelMembers,
            ObjectSchema model, Map<String, Model> modelsMap) {

        List<ClassMembers> classMembers = new ArrayList<>();

        for (Map.Entry<String, Schema> modelMapObject : modelMembers.entrySet()) {
            String variablName = modelMapObject.getKey();

            ClassMembers classMemberObject = new ClassMembers();
            Schema property = modelMembers.get(variablName);

            if (property instanceof ArraySchema) {
                classMemberObject = getClassMember((ArraySchema) property, model, modelsMap, variablName);
            } else if (property instanceof RefProperty) {
                classMemberObject = getClassMember((RefProperty) property, model, modelsMap, variablName, false);
            } else {
                classMemberObject.setDataType(
                        getDataType(property.getFormat() != null ? property.getFormat() : property.getType(), false));
                classMemberObject.setName(variablName);
            }

            classMembers.add(classMemberObject);
        }

        return classMembers;
    }

    private ClassMembers getClassMember(ArraySchema property, ObjectSchema model, Map<String, Model> models,
            String variableName) {

        ClassMembers classMemberObject = new ClassMembers();
        Schema<?> propObject = property.getItems();

        if (propObject instanceof RefProperty) {
            classMemberObject = getClassMember((RefProperty) propObject, model, models, variableName, true);
        } else if (propObject instanceof StringSchema) {
            classMemberObject = getClassMember((StringSchema) propObject, variableName);
        }

        return classMemberObject;
    }

    private ClassMembers getClassMember(StringSchema stringProperty, String variablName) {

        ClassMembers classMemberObject = new ClassMembers();
        classMemberObject.setDataType(getDataType(stringProperty.getType(), true));
        classMemberObject.setName(variablName);

        return classMemberObject;
    }

    private ClassMembers getClassMember(RefProperty refProperty, Model modelObject, Map<String, Model> models,
            String variableName, boolean fromArray) {

        ClassMembers classMemberObject = new ClassMembers();
        classMemberObject.setDataType(getDataType(refProperty.getSimpleRef(), fromArray));
        classMemberObject.setName(variableName);

        if (models.containsKey(refProperty.getSimpleRef())) {
            classMemberObject.setClassName(refProperty.getSimpleRef());
        }

        if (includeCardinality && StringUtils.isNotEmpty(variableName) && modelObject != null) {
            if (!fromArray) {
                classMemberObject.setCardinality(isRequiredProperty(modelObject, variableName) ? CARDINALITY_ONE_TO_ONE
                        : CARDINALITY_NONE_TO_ONE);
            } else {
                classMemberObject.setCardinality(isRequiredProperty(modelObject, variableName) ? CARDINALITY_ONE_TO_MANY
                        : CARDINALITY_NONE_TO_MANY);
            }
        }
        return classMemberObject;
    }

    private boolean isRequiredProperty(Model modelObject, String propertyName) {
        boolean isRequiredProperty = false;

        if (modelObject != null) {
            if (modelObject instanceof ModelImpl) {
                List<String> requiredProperties = ((ModelImpl) modelObject).getRequired();
                if (requiredProperties != null && !requiredProperties.isEmpty()) {
                    isRequiredProperty = requiredProperties.contains(propertyName);
                }
            }
        }

        return isRequiredProperty;
    }

    private String getDataType(String className, boolean isArray) {
        if (isArray) {
            return FormatUtility.toTitleCase(className) + "[]";
        }

        return FormatUtility.toTitleCase(className);
    }

}
