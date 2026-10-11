# syntax=docker/dockerfile:1

# =====================================================================
# Backend Spring Boot — immagine in due fasi.
#
#  1) build   : JDK 25 + Maven wrapper, produce il jar. Le dipendenze vengono
#               scaricate in un layer separato dai sorgenti, così una modifica
#               al codice non le riscarica.
#  2) runtime : solo JRE 25, utente non root, cartella /data/uploads per i
#               documenti (montata come volume dal docker-compose).
# =====================================================================

FROM eclipse-temurin:25-jdk AS build
WORKDIR /app

# Prima solo ciò che serve a risolvere le dipendenze (layer cache-friendly)
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
# Il wrapper deve avere fine riga Unix anche se il checkout è avvenuto su Windows
RUN sed -i 's/\r$//' mvnw && chmod +x mvnw && ./mvnw -q -B dependency:go-offline

# Poi i sorgenti e il build vero (i test girano già in locale / CI)
COPY src ./src
RUN ./mvnw -q -B package -DskipTests

# ─────────────────────────────────────────────────────────────────────
FROM eclipse-temurin:25-jre
WORKDIR /app

RUN useradd --system --uid 1001 --create-home app \
 && mkdir -p /data/uploads \
 && chown -R app:app /data /app

COPY --from=build --chown=app:app /app/target/*.jar app.jar

USER app
EXPOSE 8080

# Il profilo e le variabili (DB_URL, DB_PASSWORD, JWT_SECRET, ...) arrivano
# dall'ambiente: vedi docker-compose.yml e backend/.env.example
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "app.jar"]
