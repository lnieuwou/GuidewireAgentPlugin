#!/bin/bash
# used pw AppD123
keytool -importkeystore -srckeystore GuidewireAgentPlugin.keystore -destkeystore keys.p12 -srcstoretype jks -deststoretype pkcs12

openssl pkcs12 -in keys.p12 -out keys.pem
