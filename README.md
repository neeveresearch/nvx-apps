# The Apps Repository

This repository contains domain specific apps authored using the X Platform.

## Building the apps
1. Ensure Maven is installed
2. Ensure you have a valid X Platform license (see [Working with X Platform Licenses](https://docs.neeveresearch.com/display/TALONDOC/Working+with+X+Platform+Licenses))
2. Clone this repository
3. Run mvn install (or mvn -DskipTests clean install to skip tests). 

You may also change into a subdirector and build any application on its own. 

## The Apps
The following lists and describes the apps in this repository

### Low Latency FinServ Order Management System (nvx-app-oms)
This is a simple financial service order management application. 

### Low Latency Smart Order Routing (nvx-app-tick-to-trade)
This is a simple financial service execution management system which includes a smart order router. This application uses Hornet with type based routing.

### Stream Processing Ad Bidding Exchange (nvx-app-adbidding-engine)
This application is based on ad bidding solution donated by Kode41 which showcases the ease of developing a low latency ad exchange using the X Platform.

### Microservices Bookstore App (nvx-app-bookstore)
This application uses Eagle to implement a classic "Bookstore App" e-commerce usecase using a microservices architecture.

### Credit Card Fraud Detection (nvx-app-ccfd)
This project is based on card processing solution donated by Kode41 which showcases a fraud detection usecase using the X Platform. 
