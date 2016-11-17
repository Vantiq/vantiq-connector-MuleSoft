<div style="height: 50px"><img style="float:right" alt="Vantiq Logo" src="http://vantiq.com/wp-content/uploads/2015/12/vantiq.png"/></div>

# Vantiq Anypoint Connector

The Vantiq connector provides integration with the Vantiq platform.

## Processors

The Vantiq connector supports the followings means for publishing data from Anypoint to Vantiq.

### `publishData`

The `publishData` processor allows well defined data types to be published into Vantiq.
The data types must be pre-defined within Vantiq that matches the data being pushed.  These
pre-defined types are generally available through *Vantiq Adapters*.

DataSense is supported that provides metadata of the Vantiq data types into Anypoint.  The data published into Vantiq is handled by the *MuleSoft Connector* in Vantiq and initiates the
appropriate *Vantiq Adapter* associated with the given data type.

### `publishTopic`

To publish ad-hoc data into Vantiq, the `publishTopic` processor provides the means for 
triggering an event in Vantiq on a given topic.  Rules within Vantiq may be used to listen
for the given event.

## Sources

The Vantiq connector supports creating messages within Anypoint from Vantiq using the
given sources.

### `subscribeTopic`

The `subscribeTopic` source creates a Websocket connection to Vantiq and listens on 
a specific Vantiq topic.  Any event on that topic in Vantiq triggers a message in
Anypoint.

### `subscribeType`

The `subscribeType` source creates a Websocket connection to Vantiq and listens 
for data type events on a specific Vantiq data type.  The subscription may listen
for INSERT, UPDATE, or DELETE events.  Each event triggers a message in Anypoint.

### `selectData`

The `selectData` source polls Vantiq for data based on a specific query.  The
result of the query triggers a message in Anypoint.

# Mule supported versions

Mule 3.6+

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
