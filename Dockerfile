FROM openjdk:8-jre-alpine
MAINTAINER papubhat@gmail.com
RUN apk update && apk add bash && apk add openssl
# Install AWS CLI for TLS bootstrapping
RUN apk add python curl jq
RUN curl -O https://bootstrap.pypa.io/get-pip.py && python get-pip.py && rm get-pip.py && \
    pip install awscli
VOLUME /opt/data
ADD target/search-exec.jar search-exec.jar
EXPOSE 8092
ENTRYPOINT ["java","-jar","search-exec.jar"]