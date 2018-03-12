#!/bin/bash

if [ "$JAVA_HOME" = "" ]
then
   echo The JAVA_HOME environment variable needs to be set.
   exit 1
fi

pushd `dirname $0` > /dev/null

if [ "$#" -lt 1 ]
then
  echo "Usage: run.sh <comma-separated-profiles>"
  echo " For example:"
  echo " numactl -m1 ./ems1.sh direct"
  popd > /dev/null
  exit 1
fi

PROFILES=$1
shift

# FOR TIMESTAMPED OUTPUT FILES
TIMESTAMP=`date +%H_%M_%m%d%y`
SERVER=ems1

GC_ARGS="-XX:+PrintGCDetails -XX:+PrintGCTimeStamps -XX:+PrintTenuringDistribution -Xms8g -Xmx8g -XX:NewSize=1536m -XX:MaxNewSize=1536m -XX:SurvivorRatio=6 -XX:+UseParNewGC -XX:ParallelGCThreads=12 -Xnoclassgc -XX:MaxTenuringThreshold=2"

# Set the native library path
export LD_LIBRARY_PATH=$LD_LIBRARY_PATH:~/.nvx/native

# Delete recovery data for clean start
rm -rf rdat
mkdir -p rdat/logs

# Uncomment to enabled flight recorder
#FLIGHT_RECORDER_OPTS="-XX:+UnlockCommercialFeatures -XX:+FlightRecorder -XX:StartFlightRecording=name=MyRecording,settings=profile -XX:FlightRecorderOptions=defaultrecording=true,dumponexit=true,dumponexitpath=$SERVER-recording.jfr"

$JAVA_HOME/bin/java $FLIGHT_RECORDER_OPTS $GC_ARGS -Dnv.ddl.profiles=$PROFILES -cp "libs/*" com.neeve.server.Main -n $SERVER $* 2>&1 | tee rdat/logs/$SERVER-server-$TIMESTAMP.log

popd > /dev/null
