package org.openapi2puml.openapi.plantuml.vo;

import java.util.List;

public class ClassDiagram {

  private String className;
  private boolean isClass;
  private String description;
  private List<ClassMembers> fields;
  private List<ClassRelation> childClasses;
  private String superClass;

  public ClassDiagram(String className, String description, List<ClassMembers> fields,
                      List<ClassRelation> childClasses, boolean isClass, String superClass) {
    super();
    this.className = className;
    this.description = description;
    this.fields = fields;
    this.childClasses = childClasses;
    this.isClass = isClass;
    this.superClass = superClass;
  }

  public String getClassName() {
    return className;
  }

  public void setClassName(String className) {
    this.className = className;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public List<ClassMembers> getFields() {
    return fields;
  }

  public void setFields(List<ClassMembers> fields) {
    this.fields = fields;
  }

  public List<ClassRelation> getChildClasses() {
    return childClasses;
  }

  public void setChildClasses(List<ClassRelation> childClasses) {
    this.childClasses = childClasses;
  }

  public boolean isClass() {
    return isClass;
  }

  public void setClass(boolean isClass) {
    this.isClass = isClass;
  }

  public String getSuperClass() {
    return superClass;
  }

  public void setSuperClass(String superClass) {
    this.superClass = superClass;
  }

  @Override
  public String toString() {
    return "ClassDiagram [className=" + className + ", isClass=" + isClass + ", description=" + description
        + ", fields=" + fields + ", childClasses=" + childClasses + ", superClass=" + superClass + "]";
  }
}