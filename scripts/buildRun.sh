#!/bin/bash

cd ../
# -q is for running task in silent.
./gradlew clean build -q
./gradlew run -q < test1.txt
