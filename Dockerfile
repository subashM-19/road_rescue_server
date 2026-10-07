FROM gradle:8.7-jdk17 AS builder

WORKDIR /app

COPY . .

RUN gradle clean build --no-daemon


FROM tomcat:9.0-jdk17-temurin

RUN rm -rf /usr/local/tomcat/webapps/*

COPY --from=builder /app/build/libs/*.war /usr/local/tomcat/webapps/app.war

RUN sed -i 's/port="8080"/port="${port.http}"/' /usr/local/tomcat/conf/server.xml

ENV JAVA_OPTS="-Dport.http=10000"

EXPOSE 10000

CMD ["catalina.sh", "run"]