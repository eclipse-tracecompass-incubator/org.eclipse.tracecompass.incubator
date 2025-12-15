# Building Eclipse Trace Compass incubator

## Compiling manually

The Maven project build requires version 3.9 or later. It can be downloaded from
<http://maven.apache.org> or from the package management system of your distro.

It also requires Java version 17 or later.

To build the project manually using Maven, simply run the following command from
the top-level directory:

```bash
mvn clean install
```

The default command will compile and run the unit tests. Running the tests can
take some time, to skip them you can append `-Dmaven.test.skip=true` to the
`mvn` command:

```bash
mvn clean install -Dmaven.test.skip=true
```

The resulting executables will be in the
`rcp/org.eclipse.tracecompass.incubator.rcp.product/target/products`. There are
the archives for linux, macos and Windows. The sub-directories
`org.eclipse.tracecompass.incubator.rcp/<os>/...` contain the executable for each
OS.

To generate the javadoc from the Trace Compass source code, run the following
command from the top-level directory:

```bash
mvn clean package javadoc:aggregate
```

The javadoc html files will be under `target/site/apidocs`.

## Generate Trace Server Protocol Client (in Java)

The Trace Server test plug-in `trace-server/org.eclipse.tracecompass.incubator.jersey.rest.core.tests` uses a generated [Trace Server Protocol][trace-server-protocol] client written in Java to test the trace server TSP API. The client code is part of the plug-in `trace-server/org.eclipse.tracecompass.incubator.tsp.client.core` and can be found under `trace-server/org.eclipse.tracecompass.incubator.tsp.client.core/target/generated-sources/openapi`.

### Download the latest TSP specification

The currently used specification is (API.yaml) is downloaded to `trace-server/org.eclipse.tracecompass.incubator.tsp.client.core/` and under version control. To download a new version from the [Trace Server Protocol][trace-server-protocol] repository execute the following command. 

```bash
mvn validate -PdownloadTsp
```

### Generate the latest client code

The generation of the trace server client code from the specification (API.yaml) is done using the maven plugin of the [OpenAPI Generator][openapi-codegen]. To re-generate the TSP client execute the following command:

```bash
mvn generate-sources -PdownloadTsp -PbuildTsp
```

Notes:
- You can skip the profile to download the TSP if has been done separately
- If the API.yaml file didn't change the generation of TSP Java client is skipped.
- If you want to regenerate the TSP Java, add `-DskipIfTspIsUnchanged=false` to the maven command-line

## Maven profiles and properties

The following Maven profiles and properties are defined in
the build system. You can set them by using `-P[profile name]` and
`-D[property name]=[value]` in `mvn` commands.

- `-Pdeploy-update-site`

  Mainly for use on build servers. Copies the standard update site (for the
  Eclipse plugin installation) to the destination specified by
 `-DsiteDestination=/absolute/path/to/destination`.

- `mvn javadoc:aggregate`

  Mainly for use on build servers. Generates the javadoc of API classes as a
  HTML website to the destination specified by `-Djavadoc-site=/absolute/path/to/destination`.

## Build Trace Compass Server image with Docker

To compile the image of Trace Compass Server with Docker, run the following command from the top level directory:

```bash
docker build -t trace-server .
```

The image will be tagged `trace-server`.

To run the Trace Compass Server with the image, run the following command:

```bash
docker run -p 8080:8080 trace-server
```

The Trace Compass Server will run on port `8080`.

## Test Trace Compass Server image

You can clone the [Tracecompass Test Traces][tracecompass-test-traces] repository.

> We cloned the repository inside the `$HOME/ws` folder on our computer.

To test the image of Trace Compass Server with Docker, you need to mount traces inside the container. We will use the kernel trace from the repository, located under `$HOME/ws/tracecompass-test-traces/ctf/src/main/resources/kernel` on our computer. Then run the following command:

```bash
docker run -p 8080:8080 -v $HOME/ws/tracecompass-test-traces/ctf/src/main/resources/kernel:/traces/kernel trace-server
```

The Trace Compass Server will run on port `8080` and have the traces mount at `/traces/kernel` folder inside the container.

You can check the Trace Compass Server status using the following command:

```bash
curl -X GET 'http://localhost:8080/tsp/api/health'
```

You can open the kernel trace (or any mounted one) inside the container using the following command:

```bash
curl -X POST 'http://localhost:8080/tsp/api/traces' --header 'Content-Type: application/json' --data-raw '{ "parameters": { "uri": "/traces/kernel" } }'
```

You can create an experiment with the kernel trace (or any opened trace) using the command below. Replace the UUID with the UUID returned by the command above:

```bash
curl -X POST 'http://localhost:8080/tsp/api/experiments' --header 'Content-Type: application/json' --data-raw '{ "parameters": { "name": "Experiment Name", "traces": ["d49d04f5-9db5-3773-ace4-1594b87db661"] } }'
```

You can get all the experiments using the following command:

```bash
curl -X GET 'http://localhost:8080/tsp/api/experiments' --header 'Content-Type: application/json'
```

Refer to the [Trace Server Protocol][trace-server-protocol] for more endpoints.

[tracecompass-test-traces]: https://github.com/eclipse-tracecompass/tracecompass-test-traces
[trace-server-protocol]: https://github.com/eclipse-cdt-cloud/trace-server-protocol
[openapi-codegen]: https://openapi-generator.tech/docs/generators/java