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
  echo " numactl -m1 ./market.sh direct"
  popd > /dev/null
  exit 1
fi

PROFILES=$1
shift

# FOR TIMESTAMPED OUTPUT FILES
TIMESTAMP=`date +%H_%M_%m%d%y`
SERVER=market

GC_ARGS="-XX:+PrintGCDetails -XX:+PrintGCTimeStamps -XX:+PrintTenuringDistribution -Xms1572m -Xmx1572m -XX:NewSize=1024m -XX:MaxNewSize=1024m -XX:SurvivorRatio=32 -XX:+UseParNewGC -XX:ParallelGCThreads=3 -Xnoclassgc -XX:MaxTenuringThreshold=5"

# Set the native library path
export LD_LIBRARY_PATH=$LD_LIBRARY_PATH:~/.nvx/native

# Delete recovery data for clean start
rm -rf rdat
mkdir -p rdat/logs

#FLIGHT_RECORDER_OPTS="-XX:+UnlockCommercialFeatures -XX:+FlightRecorder -XX:StartFlightRecording=name=MyRecording,settings=profile -XX:FlightRecorderOptions=defaultrecording=true,dumponexit=true,dumponexitpath=$SERVER-recording.jfr"

$JAVA_HOME/bin/java $FLIGHT_RECORDER_OPTS $GC_ARGS -Dnv.ddl.profiles=$PROFILES -cp "libs/*" com.neeve.server.Main -n $SERVER $* 2>&1 | tee rdat/logs/$SERVER-server-$TIMESTAMP.log

popd > /dev/null
