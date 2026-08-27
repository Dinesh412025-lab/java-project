FROM eclipse-temurin:17-jdk

WORKDIR /app

COPY src ./src
COPY data ./data

RUN mkdir -p out && \
    javac --add-modules jdk.httpserver \
    -d out \
    $(find src -name "*.java")

EXPOSE 10000

CMD ["java", "--add-modules", "jdk.httpserver", "-cp", "out", "com.medgrid.Main"]
