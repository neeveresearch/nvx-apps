# The Apps Repository

This repository contains the domain specific apps authored using the X Platform

## Building the apps
1. Ensure Maven is installed
2. Ensure you have a valid X Platform license (see https://docs.neeveresearch.com/display/TALONDOC/Working+with+X+Platform+Licenses)
2. Clone this repository
3. Run mvn install

## The Apps
The following lists and describes the apps in this repository

### nvx-app-oms
This is a simple, Talon based, financial service order management application. It accepts new orders, adds orders to an order table and dispatches order events to indicate receipt of new order events

### nvx-app-bookstore
This is a simple, Eagle based, bookstore app. It enables CRUD operations on carts, allows for books to be added/removed from carts and dispatches cart change events consumed by a marketing service
