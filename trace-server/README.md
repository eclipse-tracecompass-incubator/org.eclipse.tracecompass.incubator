# Eclipse Trace Compass Server

## Table of Contents

<!-- START doctoc generated TOC please keep comment here to allow auto update -->
<!-- DON'T EDIT THIS SECTION, INSTEAD RE-RUN doctoc TO UPDATE -->

- [Compiling manually](#compiling-manually)
- [Running the server](#running-the-server)
- [Run the Server with SSL](#running-the-server-with-ssl)

<!-- END doctoc generated TOC please keep comment here to allow auto update -->

## Compiling manually

The Maven project build requires version 3.3 or later. It can be downloaded from
<http://maven.apache.org> or from the package management system of your distro.

To build the trace-server manually using Maven, simply run the following command
from the git project top-level directory:

    cd org.eclipse.tracecompass.incubator/
    mvn clean install

The default command will compile and run the unit tests. Running the tests can
take some time, to skip them you can append `-DskipTests` to the
`mvn` command:

    mvn clean install -DskipTests

The resulting executables will be in the
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
Follow the instructions to [configure SSL on jetty](https://www.eclipse.org/jetty/documentation/current/configuring-ssl.html).

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
