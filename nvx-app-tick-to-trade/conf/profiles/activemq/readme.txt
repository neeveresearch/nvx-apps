OVERVIEW
========
This configuration is meant for running multiple servers that use activemq as the messaging provider

Using ActiveMQ has the advantage of being simple to set up and run for testing and evaluation, 
but it is not a zero garbage messaging provider, and not suitable for low latency due to the 
synchronous nature of PERSISTENT messaging. The platform support high performance message buses
such as Solace that can do Guaranteed messaging in an asynchronous fashion which allows higer
messaging rates and very low latency.  

TO CONFIGURE
============
This configuration assumes that you have 3 linux boxes available, one for client and market and 
one each for the ems primary and backup vms. 

1.) client.conf
  a. Configure EMS_BUSDESCRIPTOR's address to point to your activemq instance. 
  b. Client should be launched from the same server as the market for accurate latency measurement.
  c. CPU affinitity masks:
     i.  Make sure nv.enablecpuaffinitymasks=true is set (or set to false not to affinitize) 
     ii. See Cpu Affinities below. You will want to make sure that market and client are on 
         separate nodes.
  d. Optionally configure CLIENT_ACCEPTOR to use an interface separate that the network that
     activemq is running on to isolate traffic.
          
2.) market.conf
  a. Configure EMS_BUSDESCRIPTOR's address to point to your activemq instance. 
  b. Market should be launched from the same server as the client for accurate latency measurement.
  c. CPU affinitity masks: 
     i.  Make sure nv.enablecpuaffinitymasks=true is set (or set to false not to affinitize) 
     ii. See Cpu Affinities below. You will want to make sure that market and client are on 
         separate nodes. 
  d. Optionally configure MARKET_ACCEPTOR to use an interface separate that the network that
     activemq is running on to isolate traffic.

3.) ems1.conf
  a. Configure EMS_BUSDESCRIPTOR's address to point to your activemq instance. 
  b. Configure the store's EMS_REPLICATION_INTERFACE to use an interface on the host on 
     which it will be run (preferable one other than the acceptor above. This interface is
     used for replication between ems1 and ems2 which will discover one another to form 
     a cluster
  c. CPU affinitity masks: 
     i.  Make sure nv.enablecpuaffinitymasks=true is set (or set to false not to affinitize) 
     ii. See Cpu Affinities below. You will want to make sure that market and client are on 
         separate nodes. 
  d. Optionally configure EMS1_ACCEPTOR to use an interface separate that the network that
     activemq is running on to isolate traffic.

4.) ems2.conf
  a. Configure EMS_BUSDESCRIPTOR's address to point to your activemq instance. 
  b. Configure the store's EMS_REPLICATION_INTERFACE to use an interface on the host on 
   which it will be run (preferable one other than the acceptor above. This interface is
   used for replication between ems1 and ems2 which will discover one another to form 
   a cluster
  c. CPU affinitity masks: 
     i.  Make sure nv.enablecpuaffinitymasks=true is set (or set to false not to affinitize) 
     ii. See Cpu Affinities below. You will want to make sure that market and client are on 
         separate nodes. 
  d. Optionally configure EMS2_ACCEPTOR to use an interface separate that the network that
     activemq is running on to isolate traffic.
  
CPU Affinities
==============       
When running on linux, the X Platform supports affinitizing threads to a particular cpu core.

To see the cpu layout on your machine, run:
`java -cp "libs/*" com.neeve.util.UtlThread` 

Ideally, you will want to set all of the [XX] values in the conf file to use the same socket
id. For NUMA architecture machines this allows all of the threads to share main memory attached
to the same socket which reduces latency. 

Assuming that you are running client and market on the same server to record accurate
latencies, you will want to launch each on a separate cpu socket if available. 

For example given the following output:

0: CpuInfo{socketId=0, coreId=0, threadId=0}
1: CpuInfo{socketId=0, coreId=1, threadId=0}
2: CpuInfo{socketId=0, coreId=2, threadId=0}
3: CpuInfo{socketId=0, coreId=3, threadId=0}
4: CpuInfo{socketId=0, coreId=4, threadId=0}
5: CpuInfo{socketId=0, coreId=8, threadId=0}
6: CpuInfo{socketId=0, coreId=9, threadId=0}
7: CpuInfo{socketId=0, coreId=10, threadId=0}
8: CpuInfo{socketId=0, coreId=11, threadId=0}
9: CpuInfo{socketId=0, coreId=12, threadId=0}
10: CpuInfo{socketId=1, coreId=0, threadId=0}
11: CpuInfo{socketId=1, coreId=1, threadId=0}
12: CpuInfo{socketId=1, coreId=2, threadId=0}
13: CpuInfo{socketId=1, coreId=3, threadId=0}
14: CpuInfo{socketId=1, coreId=4, threadId=0}
15: CpuInfo{socketId=1, coreId=8, threadId=0}
16: CpuInfo{socketId=1, coreId=9, threadId=0}
17: CpuInfo{socketId=1, coreId=10, threadId=0}
18: CpuInfo{socketId=1, coreId=11, threadId=0}
19: CpuInfo{socketId=1, coreId=12, threadId=0}

You'd want to set affinities for a process to reside all on cpus 0-9 or 10-19 so that they
can share the same core. 

When you launch, you should use numactl to tell linux to allocate memory only from the socket 
to wich you've affinitized. For example if you configured market threads all to use socket 0 
run with:

numactl -m0 ./market.sh direct
   
