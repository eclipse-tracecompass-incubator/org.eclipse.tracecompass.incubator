# Eclipse Trace Compass Incubator

This source tree contains the source code for Trace Compass Incubator plugins.

For information on running the Trace Compass Trace Server, refer to the [README](trace-server/README.md) in the [trace-server](trace-server) directory.

## Table of Contents

<!-- START doctoc generated TOC please keep comment here to allow auto update -->
<!-- DON'T EDIT THIS SECTION, INSTEAD RE-RUN doctoc TO UPDATE -->

- [Project rules and guidelines](#project-rules-and-guidelines)
- [Setting up the development environment](#setting-up-the-development-environment)
- [Compiling manually](#compiling-manually)
- [Maven profiles and properties](#maven-profiles-and-properties)

<!-- END doctoc generated TOC please keep comment here to allow auto update -->

## Project rules and guidelines

This project is an incubator which aims at rapid availability of new features and prototypes, to provide new functionalities to end users and get feedback from them during the development process. As such, code style and design architecture will not be looked at too much during reviews, though some advices and ideas can be discussed. The features have to work as expected though and not break anything in Trace Compass.

- Add unit tests: CI will be run on every patch on gerrit, so having unit tests ensures that a patch does not break anything somewhere else. Also, these plugins are work in progress. Having unit tests makes it easier to manipulate code structure and algorithms by knowing the expected results. Tests need to be maintained, but have more benefits than trouble.

- There is no release, so no API. Plugins may use the other incubation features at their own risk.

- The incubator is an Eclipse project so code will be verified to make sure it can be distributed by Eclipse. Committers are responsible to ensure the IP due diligence is met with every patch submitted.

When the code base of some feature gets more stable and mature, it may be ported to the main Trace Compass repository and only then will there be thorough code review and design discussion.

## Setting up the development environment

To set up the environment to build Trace Compass from within Eclipse, see this
wiki page:
<http://wiki.eclipse.org/Trace_Compass/Development_Environment_Setup>

Once the Trace Compass environment set, import the projects from this repository.

## Compiling manually

The Maven project build requires version 3.3 or later. It can be downloaded from
<http://maven.apache.org> or from the package management system of your distro.

To build the project manually using Maven, simply run the following command from
the top-level directory:

    mvn clean install

The default command will compile and run the unit tests. Running the tests can
take some time, to skip them you can append `-Dmaven.test.skip=true` to the
`mvn` command:

    mvn clean install -Dmaven.test.skip=true

The resulting executables will be in the
`rcp/org.eclipse.tracecompass.incubator.rcp.product/target/products`. There are
the archives for linux, macos and Windows. The sub-directories
`org.eclipse.tracecompass.incubator.rcp/<os>/...` contain the executable for each
OS.

To generate the javadoc from the Trace Compass source code, run the following
command from the top-level directory:

    mvn clean package javadoc:aggregate

The javadoc html files will be under `target/site/apidocs`.

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

    docker build -t trace-server .

The image will be tagged `trace-server`.

To run the Trace Compass Server with the image, run the following command:

    docker run -p 8080:8080 trace-server

The Trace Compass Server will run on port `8080`.

## Test Trace Compass Server image

You can clone the [Tracecompass Test Traces][tracecompass-test-traces] repository.

> We cloned the repository inside the `$HOME/ws` folder on our computer.

To test the image of Trace Compass Server with Docker, you need to mount traces inside the container. We will use the kernel trace from the repository, located under `$HOME/ws/tracecompass-test-traces/ctf/src/main/resources/kernel` on our computer. Then run the following command:

    docker run -p 8080:8080 -v $HOME/ws/tracecompass-test-traces/ctf/src/main/resources/kernel:/traces/kernel trace-server

The Trace Compass Server will run on port `8080` and have the traces mount at `/traces/kernel` folder inside the container.

You can check the Trace Compass Server status using the following command:

    curl -X GET 'http://localhost:8080/tsp/api/health'

You can open the kernel trace (or any mounted one) inside the container using the following command:

    curl -X POST 'http://localhost:8080/tsp/api/traces' --header 'Content-Type: application/json' --data-raw '{ "parameters": { "uri": "/traces/kernel" } }'

You can create an experiment with the kernel trace (or any opened trace) using the following command:

    curl -X POST 'http://localhost:8080/tsp/api/experiments' --header 'Content-Type: application/json' --data-raw '{ "parameters": { "name": "Experiment Name", "traces": ["d49d04f5-9db5-3773-ace4-1594b87db661"] } }'

You can get all the experiments using the following command:

    curl -X GET 'http://localhost:8080/tsp/api/experiments' --header 'Content-Type: application/json'

Refer to the [Trace Server Protocol][trace-server-protocol] for more endpoints.

[trace-server-protocol]: https://github.com/eclipse-cdt-cloud/trace-server-protocol

[tracecompass-test-traces]: https://git.eclipse.org/r/admin/repos/tracecompass/tracecompass-test-traces
