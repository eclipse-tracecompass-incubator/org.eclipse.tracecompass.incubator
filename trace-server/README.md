# Eclipse Trace Compass Server

## Table of Contents

<!-- START doctoc generated TOC please keep comment here to allow auto update -->
<!-- DON'T EDIT THIS SECTION, INSTEAD RE-RUN doctoc TO UPDATE -->


- [Compiling manually](#compiling-manually)
- [Running the server](#running-the-server)
- [Run the Server with SSL](#run-the-server-with-ssl)

<!-- END doctoc generated TOC please keep comment here to allow auto update -->

## Compiling manually

`mvn clean install`

## Running the server

```
$ cd trace-server/org.eclipse.tracecompass.incubator.trace.server.product/target/products/traceserver/linux/gtk/x86_64/trace-compass-server/
$ ./tracecompss-server`
```

This server is an implementation of the [Trace Server Protocol](https://github.com/theia-ide/trace-server-protocol) whose [API is documented](https://theia-ide.github.io/trace-server-protocol/) using the OpenAPI REST specification.

OpenAPI REST specification:
The REST API is documented using the OpenAPI specification in the API.json file.
The file can be opened with an IDE plug-in, or Swagger tools.
For more information, see [https://swagger.io/docs/](https://swagger.io/docs/).

## Run the Server with SSL

The trace server can be run using SSL certificates. Jetty requires the certificate and private key to be in a keystore. Follow the instructions to [configure SSL on jetty](https://www.eclipse.org/jetty/documentation/current/configuring-ssl.html).

Then, you can edit the `tracecompass-server.ini` file to pass the keystore data and SSL port as parameters after the -vmargs line. For example, here's a extract of the file:

```
[...]
-vmargs
[...]
-Dtraceserver.port=8443
-Dtraceserver.keystore=/path/to/keystore
```

The following properties are supported:

* `traceserver.port`: Port to use. If not specified, the default http port is 8080 and SSL is 8443
* `traceserver.useSSL`: Should be `true` or `false`. If `true`, the `traceserver.keystore` property must be set. If left unset, it will be inferred from the other properties. If `false`, the `traceserver.keystore` and `traceserver.keystorepass` will be ignored.
* `traceserver.keystore`: Path to the keystore file.
* `traceserver.keystorepass`: Password to open the keystore file. If left unset, the password will be prompted when running the trace server application.