OVERVIEW
========
This configuration is meant for running on a local developer workstation. 

This configuration launches all servers embedded in a single process which is handy for running
functional tests. It uses the platform's built in loopback bus which is an in memory router for
messages. 

This profile sets nv.conservecpu=true to avoid spinning threads to avoid overly taxing developers'
cpus

TO CONFIGURE
============
This configuration doesn't require any host specific tuning. 

You may want to tune the number of orders sent by the client by choosing different values for:

simulator.ems.orderPreallocateCount
simulator.sendCount

...to send more orders from the client. 