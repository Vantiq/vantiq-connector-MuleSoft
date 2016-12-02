# MuleSoft Connector

The [MuleSoft](https://www.mulesoft.com/) connector provides support for connecting to 
3rd party systems through MuleSoft Anypoint.  MuleSoft is an integration platform that 
can be deployed on-premises or in the cloud.  This connector provides bi-directional 
communication between Vantiq and the services exposed through MuleSoft.

## Dependencies

- [Vantiq connector common](https://github.com/Vantiq/vantiq-connector-common)

Note: The MuleSoft connector itself provides connectivity between Vantiq and MuleSoft.  
One or more adapters are required that define the specific data types and control
actions associated with the services exposed through MuleSoft.

## Install

This connector consists of the following:

- [Vantiq Anypoint Connector](external/vantiq-mule-connector) that
provides the MuleSoft Anypoint side of the integration that allows Vantiq to participate in the 
Anypoint flows.
- [MuleSoft Connector](vantiq) that provides support for the Vantiq side of the connector.

### Vantiq Anypoint Connector

See the [Vantiq Anypoint Connector README](external/vantiq-mule-connector/README.md) on installation and execution details.

### MuleSoft Connector

The MuleSoft connector can be imported into a Vantiq namespace using the [Vantiq connector common](https://github.com/Vantiq/vantiq-connector-common) `vantiq-import-adapter.sh` script:

    % vantiq-import-adapter.sh https://github.com/Vantiq/vantiq-connector-MuleSoft.git

Or directly using the CLI:

    % git clone https://github.com/Vantiq/vantiq-connector-MuleSoft.git
    % cd vantiq-connector-MuleSoft/vantiq
    % vantiq -s <profile> import

where `<profile>` provides the credentials for the Vantiq CLI.

# Copyright and License

Copyright &copy; 2016 Vantiq, Inc.  Code released under the [MIT License](./LICENSE)
