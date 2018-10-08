FROM java:8-alpine
MAINTAINER Eric Fode <you@example.com>

ADD target/uberjar/grownome.jar /grownome/app.jar

EXPOSE 3000

CMD ["java", "-jar", "/grownome/app.jar"]
