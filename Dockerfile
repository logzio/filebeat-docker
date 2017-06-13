FROM alpine:3.5

MAINTAINER Moshe Eshel <moshe.eshel@kenshoo.com>

ENV FILEBEAT_VERSION=5.4.0

RUN mkdir -p /opt
RUN apk update && apk add bash wget \
    && wget --no-check-certificate https://artifacts.elastic.co/downloads/beats/filebeat/filebeat-${FILEBEAT_VERSION}-linux-x86_64.tar.gz -O /opt/filebeat.tar.gz \
    && cd /opt  \
    && tar xzvf filebeat.tar.gz  \
    && cd filebeat-*  \
    && cp filebeat /bin  \
    && cd /opt  \
    && rm -rf filebeat*  \
    && apk del wget \
    && rm -rf /var/cache/apk/*

COPY docker-entrypoint.sh /
ENTRYPOINT ["/docker-entrypoint.sh"]

RUN mkdir -p /etc/pki/tls/certs;

# Set the CA file
ADD files/COMODORSADomainValidationSecureServerCA.crt /etc/pki/tls/certs/

# Add scripts
ADD scripts/* /root/

CMD ["/root/go.sh"]

