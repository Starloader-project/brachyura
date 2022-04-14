#!/bin/bash
set -e
cd brachyura
mvn clean package verify -Dmaven.test.skip=true --offline
cd ..
cd bootstrap
mvn clean package verify -Dmaven.test.skip=true --offline
cd ..
cd build
mvn clean package verify -Dmaven.test.skip=true --offline
java -Dde.geolykt.starloader.brachyura.build.offline=true -jar ./target/brachyura-build-0.jar
cd ..
