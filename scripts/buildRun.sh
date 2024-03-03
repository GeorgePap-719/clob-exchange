#!/bin/bash

cd ../
# Set variable input_file to either $1 or /dev/stdin, in case $1 is empty
# Note that this assumes that you are expecting the file name to operate on on $1
input_file="${1:-/dev/stdin}"
# -q is for running task in silent.
./gradlew clean build -q
./gradlew run -q < "$input_file"
