FROM eclipse-temurin:25-jdk AS build
WORKDIR /app

COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN chmod +x mvnw

COPY src/ src/
COPY frontend/ frontend/

RUN ./mvnw -B clean package -DskipTests

FROM eclipse-temurin:25-jre AS runtime
WORKDIR /app
COPY --from=build /app/target/portfolio-*.jar app.jar

# Correr como usuario sin privilegios (no root)
RUN groupadd -r app && useradd -r -g app app
USER app

EXPOSE 8080
# --enable-native-access: Netty usa metodos nativos restringidos y desde Java 24
# eso emite un warning en cada arranque salvo que se autorice explicitamente.
ENTRYPOINT ["java", "--enable-native-access=ALL-UNNAMED", "-jar", "app.jar"]
