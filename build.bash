#!/bin/bash
set -e
cd cfr
mvn -Dmaven.compiler.fork=true package install
cd ..
cd fabricmerge
mvn clean package verify install
cd ..
cd access-widener
mvn clean package verify install
cd ..
cd crabloader
mvn clean package verify install
cd ..
cd trieharder
mvn clean package verify install
cd ..
cd brachyura-mixin-compile-extensions
# needs to be seperate or service loader will explode?
mvn clean package
mvn clean install
cd ..
cd fernutil
mvn clean package verify install
cd ..
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
