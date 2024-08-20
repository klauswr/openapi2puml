
#Build and push klauswr/openapi2puml:latest
```
mvn package
docker build -t klauswr/openapi2puml:latest .
docker push klauswr/openapi2puml:latest
```
docker run -e JAVA_TOOL_OPTIONS='-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:8000' -p 7000:7000 -p 7001:8000 --name openapi2puml klauswr/openapi2puml:latest


# OpenAPI2Puml

[![Build Status](https://api.travis-ci.com/openapi2puml/openapi2puml.svg?branch=master)](https://travis-ci.com/openapi2puml/openapi2puml)
[![codecov](https://codecov.io/gh/openapi2puml/openapi2puml/branch/master/graph/badge.svg)](https://codecov.io/gh/openapi2puml/openapi2puml)
[![Gitpod ready-to-code](https://img.shields.io/badge/Gitpod-ready--to--code-blue?logo=gitpod)](https://gitpod.io/#https://github.com/openapi2puml/openapi2puml)

OpenApi to Plant UML conversion tool generates UML Class Diagrams from an Open API definition.

This is a fork of the original project Swagger2puml (https://github.com/kicksolutions/swagger2puml) which seems to have been dormant for some time.

The original developers of Swagger2puml are:
- Santosh Manapragada https://github.com/msantosh1188
- Manisha Bardiya https://github.com/manishabardiya

This project is based on Maven.
Following are modules we currently have

- openapi2puml-core

Following are the tools which this project internally uses:

- [Swagger Parser]
- [Plant UML]
- [Graphviz]
- [Mustache]

## How does it work

- Input: Openapi2Puml parses the swagger definition from input using [Swagger Parser]
- Transform: The swagger definition is built into an object model
- Output: The object model is transformed into a [Plant UML] file using a [Mustache] template. Optionally a .svg
image can be generated also.

### openapi2puml-core:

This utility takes OpenAPI Yaml or JSON as input generates swagger.puml and swagger.svg files as output.

Below is the Sample Class Diagram generated by the application.
To see the generated PUML file, please click [here](examples/swagger.puml)

![Swagger-Class-Diagram-Sample](examples/swagger.svg)

## Building:

```
mvn package
```

The jar is built with dependencies and placed in the root of the project.

## Usage:

```
java -jar openapi2puml.jar [options]

-i {Path of Swagger Definition (Can be either Yaml or json)}
-o {Target location where Puml File and Image should generated}
-generateDefinitionModelOnly {true/flase Defult False (Optional)}
-includeCardinality {true/flase Defult true (Optional)}
-generateSvg true/false; Default=true

```

## Dockerized version of OpenAPI2Puml

It is also to use a docker container to run the tool. Simply mount a directory
and add command line options by using `docker run`, e.g.

```bash
# Run a docker container
$ sudo docker run --rm -it --name openapi2puml \
    -v $PWD/examples:/specs \
    openapi2puml/openapi2puml -i /specs/swagger.yaml -o /specs
```

A dockerhub repo exists here: https://hub.docker.com/r/openapi2puml/openapi2puml

To build and run a local copy, do the following:

```bash
# Obtain base image for build
$ sudo docker pull maven:3-jdk-11  # needed since docker-hub pull restrictions

# Build the image from the project
$ sudo docker build -t openapi2puml .

# Run a docker container
$ sudo docker run --rm -it --name openapi2puml \
    -v $PWD/examples:/specs \
    openapi2puml -i /specs/swagger.yaml -o /specs
```

## Running in GitPod

You can directly run this code in GitPod by clicking [this link](https://gitpod.io/#https://github.com/openapi2puml/openapi2puml) and Running

and then using this command line:

```bash
java -cp /workspace/openapi2puml/openapi2puml-core/target/dependency/*:/workspace/openapi2puml/openapi2puml-core/target/openapi-plantuml-core-0.0.1-SNAPSHOT.jar org.openapi2puml.openapi.OpenApi2PlantUML -i /workspace/openapi2puml/examples/swagger.yaml -o /workspace/openapi2puml/examples
```

License
----

Apache 2.0

[Plant UML]: <https://github.com/plantuml/plantuml>
[Swagger]: <https://swagger.io/>
[Swagger Parser]: <https://github.com/swagger-api/swagger-parser>
[Graphviz]: <https://graphviz.gitlab.io/>
[Mustache]: <https://github.com/spullara/mustache.java>
