# 1 - build the frontend into static files
FROM node:22-alpine AS frontend
WORKDIR /frontend
COPY tool-frontend/ ./
RUN npm install && npm run build-only

# 2 - build and run the backend (serves the UI from /resources + the /api)
FROM maven:3.9-eclipse-temurin-17
WORKDIR /build
COPY . .

# put built ui into resources/static
COPY --from=frontend /frontend/dist tool-backend/src/main/resources/static

# install sdmetrics.jar
RUN mvn -q install:install-file -Dfile=generator/lib/sdmetrics.jar \
      -DgroupId=com.sdmetrics -DartifactId=sdmetrics -Dversion=3.0-opencore -Dpackaging=jar

# build generator
RUN mvn -q -f generator/pom.xml -DskipTests install
# build backend
RUN mvn -q -f tool-backend/pom.xml -DskipTests package

# Run from /tool-backend so theres no errors cuz of relative path for conf files
WORKDIR /build/tool-backend
EXPOSE 8081
CMD ["sh", "-c", "java -jar target/tool-backend-*.jar"]
