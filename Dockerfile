FROM prima/filebeat:1

MAINTAINER Moshe Eshel <moshe.eshel@kenshoo.com>

RUN mkdir -p /etc/pki/tls/certs;

# Set the CA file
ADD files/COMODORSADomainValidationSecureServerCA.crt /etc/pki/tls/certs/

# Add scripts
ADD scripts/* /root/

CMD ["/root/go.sh"]
