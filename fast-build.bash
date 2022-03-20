#!/bin/bash
set -e
cd brachyura
mvn clean package verify -Dmaven.test.skip=true
cd ..
cd bootstrap
mvn clean package verify -Dmaven.test.skip=true
cd ..
cd build
mvn clean package verify -Dmaven.test.skip=true
java -jar ./target/brachyura-build-0.jar
cd ..
