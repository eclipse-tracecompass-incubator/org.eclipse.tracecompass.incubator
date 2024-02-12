# Eclipse Trace Compass Incubator

This source tree contains the source code for Trace Compass Incubator plugins.

For information on running the Trace Compass Trace Server, refer to the [README](trace-server/README.md) in the [trace-server](trace-server) directory.

## Table of Contents

<!-- START doctoc generated TOC please keep comment here to allow auto update -->
<!-- DON'T EDIT THIS SECTION, INSTEAD RE-RUN doctoc TO UPDATE -->

- [Project rules and guidelines](#project-rules-and-guidelines)
- [Contributing to Trace Compass](#contributing-to-trace-compass)
- [Reporting issues](#reporting-issues)
- [Help and suport](#help-and-support)

<!-- END doctoc generated TOC please keep comment here to allow auto update -->

## Project rules and guidelines

This project is an incubator which aims at rapid availability of new features and prototypes, to provide new functionalities to end users and get feedback from them during the development process. As such, code style and design architecture will not be looked at too much during reviews, though some advices and ideas can be discussed. The features have to work as expected though and not break anything in Trace Compass.

- Add unit tests: CI will be run on every patch on gerrit, so having unit tests ensures that a patch does not break anything somewhere else. Also, these plugins are work in progress. Having unit tests makes it easier to manipulate code structure and algorithms by knowing the expected results. Tests need to be maintained, but have more benefits than trouble.

- There is no release, so no API. Plugins may use the other incubation features at their own risk.

- The incubator is an Eclipse project so code will be verified to make sure it can be distributed by Eclipse. Committers are responsible to ensure the IP due diligence is met with every patch submitted.

When the code base of some feature gets more stable and mature, it may be ported to the main Trace Compass repository and only then will there be thorough code review and design discussion.

## Contributing to Trace Compass

**👋 Want to help?** Read our [contributor guide](CONTRIBUTING.md) and follow the
instructions to contribute code.

You will also find there information about the `setup of the development environment`,
`build instructions`, as well as the `development and review process`.

## Reporting issues

Read our [contributor guide](CONTRIBUTING.md#where-to-submit) to get details on
how to report issues.

## Help and support

See [contact](CONTRIBUTING.md#contact) section of the contributor guide on how to get help and support. 
