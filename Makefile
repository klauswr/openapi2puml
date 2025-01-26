SHELL := cmd

commands:
	echo make ...
	echo	install
	echo	build
	echo	verify
	echo	test        run tests
	echo	image       build image from DOCKER file for tag openapi2puml:dev
	echo	run         container with image openapi2puml:dev
	echo	push        image klauswr/openapi2puml:latest to dockerhub

.SILENT: commands

install:
	mvn clean install

build:
	mvn clean compile

verify:
	mvn clean verify

test:
	mvn test

image:
	mvn clean install
	docker build -t openapi2puml:dev .

run:
	docker run -d -p 7000:7000 --name openapi2pumldev openapi2puml:dev

push
	mvn package
	docker build -t klauswr/openapi2puml:latest .
	docker push klauswr/openapi2puml:latest