#!/bin/bash

ssh-keygen -t rsa -b 2048 -m PEM -f jwtRS256_2048.key
# Don't add passphrase
openssl rsa -in jwtRS256_2048.key -pubout -outform PEM -out jwtRS256_2048.key.pub
cat jwtRS256_2048.key
cat jwtRS256_2048.key.pub
