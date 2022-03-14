FROM adoptopenjdk/openjdk11:alpine-jre

RUN apk update && apk add bash

COPY target/universal/ferload.tgz .

RUN tar xvf ferload.tgz

ENTRYPOINT ["/ferload/bin/ferload"]