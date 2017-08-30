# nvx-app-ad-bidding-engine

Showcases Real-Time Ad Bidding System built on X Platform.

Take a look at:
* [Whitepaper](docs/x%20platform%20ad%20bidding%20white%20paper.docx?raw=true) (word)
* [Presentation](docs/nvx-ad-bidding-presentation.pptx?raw=true) (powerpoint)

## Background on Ad Bidding Systems

![Ad Bidding Flow](https://github.com/neeveresearch/nvx-app-ad-bidding-engine/raw/master/docs/AdBiddingFlow.JPG "Ad Bidding Flow")

Real-time Bidding (RTB) is a way of transacting media that allows an individual ad impression to be put up for bid in real-time. This is done through a programmatic on-the-spot auction, which is similar to how financial markets operate. RTB allows for Addressable Advertising; the ability to serve ads to consumers directly based on their demographic, psychographic, or behavioral attributes [2].

To get an overview on today’s online advertisement placement, let's take a look what happens in typical ad serving system when end-user browses and loads a web page with a banner ad placeholder.

When user’s page loads, it contains an URL through which ad should be retrieved. This URL points to publisher’s Ad Server. Ad Server may define some rules on what ad is to be served. For instance, if there is reserved advertisement space than an advertiser bought directly from publisher, the ad will be served directly from Ad Server and process ends there. If ad space is not reserved, the Ad Server may contact Supply-Side Platform (SSP) and offer to sell the ad space. The SSP may hold some data on user’s behavior and interests to aid targeted advertisement. The SSP sends an ad request to Ad Exchange, appended with any additional useful information about the user and publisher, such as content keywords.

The Ad Exchange, on receiving the ad request, will look up in Data Management Platform (DMP) for any information known about visitor, and then auction it. Ad Exchange will send bid requests to potential buyers, along with any available visitor info. Buyers are Demand-Side Platforms or other Ad Exchanges. Bidders may also pre-cache bids in bulk on the Ad Exchange. This works like setting automatic buy/short on stock exchanges when certain conditions are met. Conditions may include some data on end-user such as interests, age, or some data on the publisher current served content such as keywords. Bidders typically must respond within half of total time limit for Ad Exchange response.

Once Ad Exchange receives responses it does the following:
1.	Determines the winner
2.	Debits winner’s account for price of bid.
3.	Sends win notification to the bidder.
4.	Sends response to SSP with URL for retrieving the ad. Ad Exchange may get this URL either initially with the response of bid request, or in response to win notification sent to bidder.

SSP passes the ad URL to publisher’s ad server and ad server notifies the user’s browser how to retrieve the ad stored on Advertiser’s Ad Server.

For small publishers that use third party SSP such as Google Ad Sense, Publisher Ad Server may not exist. An ad request may be sent directly from browser to SSP.

