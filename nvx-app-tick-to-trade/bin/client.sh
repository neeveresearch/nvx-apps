#!/bin/bash

if [ "$JAVA_HOME" = "" ]
then
   echo The JAVA_HOME environment variable needs to be set.
   exit 1
fi

pushd `dirname $0` > /dev/null

if [ "$#" -lt 1 ]
then
  echo "Usage: run.sh <profile-folder>"
  echo " For example:"
  echo " numactl -m1 ./client.sh direct"
  popd > /dev/null
  exit 1
fi

APPCONF=conf/profiles/$1/client.conf
shift

GC_ARGS="-XX:+PrintGCDetails -XX:+PrintGCTimeStamps -XX:+PrintTenuringDistribution -Xms1572m -Xmx1572m -XX:NewSize=1024m -XX:MaxNewSize=1024m -XX:SurvivorRatio=32 -XX:+UseParNewGC -XX:ParallelGCThreads=3 -Xnoclassgc -XX:MaxTenuringThreshold=5"

# Set the native library path
export LD_LIBRARY_PATH=$LD_LIBRARY_PATH:~/.nvx/native

# Delete recovery data for clean start
rm -rf rdat

# Uncomment to enabled flight recorder
#FLIGHT_RECORDER_OPTS="-XX:+UnlockCommercialFeatures -XX:+FlightRecorder -XX:StartFlightRecording=name=MyRecording,settings=profile -XX:FlightRecorderOptions=defaultrecording=true,dumponexit=true,dumponexitpath=client-recording.jfr"

$JAVA_HOME/bin/java $FLIGHT_RECORDER $GC_ARGS -Dnv.app.propfile=$APPCONF -cp "libs/*" com.neeve.server.Main -n client $*

popd > /dev/null
