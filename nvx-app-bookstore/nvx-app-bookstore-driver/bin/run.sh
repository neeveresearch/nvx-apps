#!/bin/bash

if [ "$JAVA_HOME" = "" ]
then
   echo The JAVA_HOME environment variable needs to be set.
   exit 1
fi

pushd `dirname $0`
$JAVA_HOME/bin/java -Dnv.app.propfile=conf/application.conf -cp "libs/*" com.neeve.bookstore.cart.service.Driver
popd
