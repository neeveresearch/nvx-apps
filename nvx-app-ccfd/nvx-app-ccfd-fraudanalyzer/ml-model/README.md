# About this Model
The machine learning algorithm here was adapted from Currie32's excellent project on  
[kaggle](https://www.kaggle.com/currie32/predicting-fraud-with-tensorflow/notebook). 

# Changes
We tried to maka as few change to the model as possible to demonstrate how existing models can be plugged into the X platform. Among the few changes we did make:

* Removed / commented some of the demonstrative / charting steps. 
* Ability to save the model in format that the tensor flow java library can ingest
* Added a script to export the normalized data for ingestion by our applications. 
* Provided names for place holder variables so that they can be picked up from the Java API
 
# Retraining the Model
If you would like to tweak the model yourself, here are a few tips on how to get started:

## A Few Hints:
* [Python 3.6](https://www.python.org/downloads/release/python-365/)
    * You'll need the following modules:
    
```
pip3 install --upgrade tensorflow
pip3 install --upgrade pandas
pip3 install --upgrade sklearn
pip3 install --upgrade matplotlib
pip3 install --upgrade seaborn
```
          
* You'll need to delete the output/saved-model builder before re-running ccfd-model-training.py.
* You'll need to unzip creditcard.zip to the input folder. 
* If you change the set of features used, update and re-run ccfd-gen-normalize-txn-data.py and copy the output into the nvx-app-ccdd-roe/src/resources folder so that it can be used to see transaction data.
* Zip up the contenxt of the output folder, replacing the ml-output.zip ... this is bundled with fraudanalyzer app and extracted for use on demand. 
      
    
    