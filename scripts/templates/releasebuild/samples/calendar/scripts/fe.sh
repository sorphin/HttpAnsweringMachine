#!/bin/bash


HAM_VERSION=4.3.0
START_LOCATION=$(pwd)
SCRIPT_DIR=$( cd -- "$( dirname -- "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )
cd $SCRIPT_DIR
cd ..
CALENDAR_PATH=$(pwd)

# start fe
cd $CALENDAR_PATH/fe
java -jar "fe-$HAM_VERSION.jar" --spring.config.location=file://$(pwd)/application.properties &
cd $START_LOCATION

