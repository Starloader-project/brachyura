#!/bin/bash
set -e
cd brachyura
mvn clean package verify
cd ..
cd bootstrap
mvn clean package verify
cd ..
cd build
mvn clean package verify
java -jar ./target/brachyura-build-0.jar
cd ..
