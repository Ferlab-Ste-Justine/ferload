FROM amazoncorretto:17-alpine3.18

COPY target/scala-3.3.1/ferload.jar .

ENTRYPOINT ["java", "-jar", "ferload.jar"]