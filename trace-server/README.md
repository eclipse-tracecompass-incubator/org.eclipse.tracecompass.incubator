# Eclipse Trace Compass Server

## Table of Contents

<!-- START doctoc generated TOC please keep comment here to allow auto update -->
<!-- DON'T EDIT THIS SECTION, INSTEAD RE-RUN doctoc TO UPDATE -->

- [Developing for the server](#developing-for-the-server)
- [Compiling manually](#compiling-manually)
- [Running the server](#running-the-server)
- [Running the server with SSL](#running-the-server-with-ssl)

<!-- END doctoc generated TOC please keep comment here to allow auto update -->

## Developing for the server

For information on developing for the '''Trace Compass Trace Server''' see the [Trace Server Developer Guide](https://github.com/eclipse-tracecompass/org.eclipse.tracecompass/blob/master/doc/trace-server/trace-server-dev-guide.md).

## Compiling manually

To build the project manually follow the instructions in [BUILDING](../BUILDING).

The resulting trace server executables will be in the
`trace-server/org.eclipse.tracecompass.incubator.trace.server.product/target/products`.
There are the archives for linux, macos and Windows. The sub-directories
`trace-server/org.eclipse.tracecompass.incubator.trace.server.product/target/products/traceserver/<os>/...`
contain the executable for each OS.

## Running the server

    cd trace-server/org.eclipse.tracecompass.incubator.trace.server.product/target/products/traceserver/linux/gtk/x86_64/trace-compass-server/
    ./tracecompass-server

This server is an implementation of the [Trace Server Protocol](https://github.com/theia-ide/trace-server-protocol),
whose [API is documented](https://theia-ide.github.io/trace-server-protocol/) using the OpenAPI REST specification.

OpenAPI REST specification:

- The REST API is documented using the OpenAPI specification in the `API.yaml` file.
- The file can be opened with an IDE plug-in, or Swagger tools.
- For more information, see [TSP's README](https://github.com/theia-ide/trace-server-protocol/blob/master/README.md#how-to).

## Running the server with SSL

The trace server can be run using SSL certificates.
Jetty requires the certificate and private key to be in a keystore.
Follow the instructions to [configure SSL on jetty](https://jetty.org/docs/jetty/12/operations-guide/keystore/index.html).

Then, you can edit the `tracecompass-server.ini` file to pass the keystore data and SSL port as parameters after the `-vmargs` line.
For example, here is an excerpt of the file:

    [...]
    -vmargs
    [...]
    -Dtraceserver.port=8443
    -Dtraceserver.keystore=/path/to/keystore

The following properties are supported:

- `traceserver.host`: Host to use. If not specified, the default http host is 0.0.0.0.
- `traceserver.port`: Port to use. If not specified, the default http port is 8080 and SSL is 8443.
- `traceserver.useSSL`: Should be `true` or `false`. If `true`, the `traceserver.keystore` property must be set. If left unset, it will be inferred from the other properties. If `false`, the `traceserver.keystore` and `traceserver.keystorepass` will be ignored.
- `traceserver.keystore`: Path to the keystore file.
- `traceserver.keystorepass`: Password to open the keystore file. If left unset, the password will be prompted when running the trace server application.
