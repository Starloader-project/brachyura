#!/bin/bash
javac Buildscript.java
java -Dskiptests=true Buildscript publish
