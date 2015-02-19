This application demonstrates how to use the Cloudant NoSQL DB service, with 
the 'Liberty for Java™' runtime on IBM Bluemix Cloud.


You can deploy your own copy of this application using the button below:

[![Deploy to Bluemix](https://bluemix.net/deploy/button.png)](https://bluemix.net/deploy?repository=https://hub.jazz.net/git/idsorg/sample-java-cloudant)


This repository is targeted by sample buttons in documentation such as
the [Deploy to Bluemix button blog](https://developer.ibm.com/devops-services/2015/02/18/share-code-new-deploy-bluemix-button/).


The repository content was originally borrowed from the official Bluemix Web Starter for Java Cloudant sample.
Its WAR file got discarded in order to demonstrate the ability of the Deploy to Bluemix button 
to dynamically compile from sources on deploy requests.

This sample works because the code repository contains two interesting files:

* An Ant build.xml file, at the root, which causes compilation and packaging of the Java 
   code. 
* A manifest.yml file at the root, which we augmented to trigger the backing service 
   creation:
      declared-services:
        sample-java-cloudant-cloudantNoSQLDB:
          label: cloudantNoSQLDB
          plan: Shared


