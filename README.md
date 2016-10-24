<div style="height: 50px"><img style="float:right" alt="Vantiq Logo" src="http://vantiq.com/wp-content/uploads/2015/12/vantiq.png"/></div>

# Vantiq Anypoint Connector

The Vantiq connector provides integration with the Vantiq platform.

Data and messages can be published to Vantiq from Anypoint using `publishData`.  DataSense is supported that provides metadata of the Vantiq data types into Anypoint.  The data is published into Vantiq and is expected to be handled by the *Vantiq MuleSoft System Adapter*.

The `selectData` source can be used to periodically poll Vantiq for data based on a specific query.

# Mule supported versions

Mule 3.5+

# Installation 

## Local

To build and install locally, clone this repo.  Then, build and package the connector:

```
mvn clean package
```

The connector can be uploaded and installed into Anypoint Studio using Help → Install New Software.  To install from the exploded form, install from:

```
target/update-site
```

To install from the Zip file form, install from:

```
target/UpdateSite.zip
```

## Update Site

For released connectors you can download them from the update site in Anypoint Studio. 
Open Anypoint Studio, go to Help → Install New Software and select Anypoint Connectors Update Site where you’ll find all avaliable connectors.

# Copyright and License

Copyright &copy; Vantiq, Inc.  Code released under the [MIT license](./LICENSE.md).
