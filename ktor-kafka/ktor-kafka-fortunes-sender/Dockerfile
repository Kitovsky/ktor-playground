FROM bellsoft/liberica-openjdk-alpine:17.0.3
ADD build/libs/service-all.jar /tmp/service.jar
ENV PORT 8080
CMD ["sh", "-c", "java -jar -XX:MaxJavaStackTraceDepth=10 -XX:-OmitStackTraceInFastThrow /tmp/service.jar"]
