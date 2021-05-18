FROM openjdk:11

COPY target/universal/ferload.tgz .

RUN tar xvf ferload.tgz

ENTRYPOINT ["/ferload/bin/ferload"]