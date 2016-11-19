# Simple Roundtrip Demo Application

This demo application demostrates a round trip that originates from the Vantiq
system through a MuleSoft then back into MuleSoft.

## Details

### Demo Adapter

In the `src/main/vantiq`, there is a demo Vantiq adapter that uses the MuleSoft
connector to provide the Vantiq side of the integration for the demo.

* `Control_MuleDemo_TriggerMessage`: The procedure that will trigger a message
from Vantiq into MuleSoft through a subscription.
* `Adapter_MuleDemo_TargetType`: The rule that is triggered from the `publishData`.
* `MuleDemo_TargetType`: The Vantiq data type that will be published from MuleSoft

#### Install Adapter

To install the adapter, first install the [Vantiq-connector-common](https://github.com/Vantiq/vantiq-connector-common.git) using the [Vantiq CLI](https://dev.vantiq.com/downloads/vantiq.zip).

```
% git clone https://github.com/Vantiq/vantiq-connector-common.git
% cd vantiq-connector-common
% vantiq -s <profile> import
```

Then, install the MuleDemo adapter.

```
% cd src/main/vantiq
% vantiq -s <profile> import
```

### Vantiq Connector

The Vantiq MuleSoft connector artifacts need to be installed to support inbound `publishData` requests
into the system.  Note this is the Vantiq-side of the connector.

```
% cd ../../vantiq
% vantiq -s <profile> import
```

### Demo Roundtrip MuleSoft App

The `demo-roundtrip` MuleSoft app is designed to listen for `MuleDemo_TriggerMessage`
actions and publish the data back into Vantiq into the `MuleDemo_TargetType` data type.

## Running the Demo

To perform the demo roundtrip, follow the steps:

1. Start the `demo-roundtrip` project
    a. Start the Anypoint Studio
    b. Import the `demo-roundtrip` project
    c. Ensure the `Vantiq connector` configurations include the correct credentials
    d. Run the `demo-roundtripFlow` flow

2. Trigger the `TriggerMessage` control action
    a. Log into the Vantiq system
    b. Navigate to the *Develop > Procedures* page
    c. Execute the `Control_DemoMule_TriggerMessage` procedure
    d. Enter a string value for `msg` and integer value for `value`

3. Verify Result
    a. Check the Anypoint Studio Console to ensure the `demo-roundtripFlow` executed the flow
    b. Navigate to Vantiq *Data > Find Records*
    c. Choose *DemoMule_TargetType*
    d. Click `Run Query`
    e. Verify a record was created with the given `msg` and `value` fields
