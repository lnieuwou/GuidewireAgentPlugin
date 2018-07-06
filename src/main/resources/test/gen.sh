#!/bin/bash

pw=AppD123

## 1. Create certificate authority (CA)
openssl req -new -x509 -keyout ca-key -out ca-cert -days 10000 -passin pass:$pw -passout pass:$pw -subj "/CN=appdynamics.com/OU=PS/O=AppDynamics/L=London/ST=London/C=UK"

## 2. Create client keystore
keytool -noprompt -keystore plugin.jks -genkey -alias appdguidewireplugin -genkey -v -keyalg RSA -keysize 2048 -dname "CN=appdynamics.com, OU=PS, O=AppDynamics, L=London, ST=London, C=UK" -storepass $pw -keypass $pw

## 3. Sign client certificate
keytool -noprompt -keystore plugin.jks -alias appdguidewireplugin -certreq -file cert-unsigned -storepass $pw
openssl x509 -req -CA ca-cert -CAkey ca-key -in cert-unsigned -out cert-signed -days 10000 -CAcreateserial -passin pass:$pw

## 4. Import CA and signed client certificate into client keystore
keytool -noprompt -keystore plugin.jks -alias CARoot -import -file ca-cert -storepass $pw
keytool -noprompt -keystore plugin.jks -alias appdguidewireplugin -import -file cert-signed -storepass $pw

## 5. Import CA into client truststore (only for debugging with Java consumer)
keytool -noprompt -keystore plugin.jks -alias CARoot -import -file ca-cert -storepass $pw

## 6. Import CA into server truststore
# keytool -noprompt -keystore cacerts -alias CARoot -import -file ca-cert -storepass changeit

## 7. Create PEM files for Ruby client
### 7.1 Extract signed client certificate
#keytool -noprompt -keystore kafka.client.keystore.jks -exportcert -alias localhost -rfc -storepass foobar -file client_cert.pem
### 7.2 Extract client key
#keytool -noprompt -srckeystore kafka.client.keystore.jks -importkeystore -srcalias localhost -destkeystore cert_and_key.p12 -deststoretype PKCS12 -srcstorepass foobar -storepass foobar
#openssl pkcs12 -in cert_and_key.p12 -nocerts -nodes -passin pass:foobar -out client_cert_key.pem
### 7.3 Extract CA certificate
#keytool -noprompt -keystore kafka.client.keystore.jks -exportcert -alias CARoot -rfc -file ca_cert.pem -storepass foobar
