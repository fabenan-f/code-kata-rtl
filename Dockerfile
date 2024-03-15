FROM maven:3-eclipse-temurin-17

WORKDIR /usr/src/app
COPY . .

RUN mvn install -DskipTests

CMD ["mvn", "spring-boot:run" ]
