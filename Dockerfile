FROM gradle:9.8.2-jdk25-noble

WORKDIR /workspace
USER root

COPY build.gradle settings.gradle ./
COPY src ./src

EXPOSE 8080

CMD ["gradle", "--no-daemon", "--console=plain", "bootRun"]
