# Contributing to Eclipse Trace Compass Incubator

Thanks for your interest in this project. This page explains how to contribute code to the Trace Compass Incbator project.

The Trace Compass Incubator is a project that contains additional features:

- That are under development, but still usable enough to be used and tested by users.
- Whose content relates to a specific trace type or domain of analysis (for example virtual machine analyses) and that no other plugin will depend on.

This project is a permanent incubator, i.e. the features will never be officially released with a specific version, hence, nothing will be API in the plugins developed there. Some feature may eventually graduate to the Trace Compass project itself if required, for example if many other features depend on them or they provide an interesting functionality for the core Trace Compass.

## Terms of Use

This repository is subject to the [Terms of Use of the Eclipse Foundation][terms].

## Code of Conduct

This project is governed by the [Eclipse Community Code of Conduct][code-of-conduct].
By participating, you are expected to uphold this code.

## Eclipse Development Process

This Eclipse Foundation open project is governed by the [Eclipse Foundation Development Process][dev-process] and operates under the terms of the [Eclipse IP Policy][ip-policy].

## Eclipse Contributor Agreement

In order to be able to contribute to Eclipse Foundation projects you must electronically sign the [Eclipse Contributor Agreement (ECA)][eca].

The ECA provides the Eclipse Foundation with a permanent record that you agree that each of your contributions will comply with the commitments documented in the Developer Certificate of Origin (DCO). Having an ECA on file associated with the email address matching the "Author" field of your contribution's Git commits fulfills the DCO's requirement that you sign-off on your contributions.

For more information, please see the [Eclipse Committer Handbook][commiter-handbook].

## Source code tree

This source tree contains the source code for the Trace Compass incubator plugins for Eclipse.

The plug-ins are categorized as follows:

```text
    analysis/     | Analysis extensions to the base framework
    common/       | Basic release engeneering (e.g. target definitions, update site)
    doc/          | Documentation and code examples
    rcp/          | Code specific to the RCP version
    scripting     | Plug-ins providing scripting capabilities, e.g. javascript, Python using EASE
    skeleton/     | Skeleton plug-ins for adding new plug-ins/features to the repository
    trace-server/ | Trace Compass server plug-ins and RCP
    tracetypes/   | Plug-ins providing new trace types (e.g. ftrace, ROS2)
    vm/           | LTTng virtual machine analysis
```

## Getting started

So, you have a new feature to propose. First follow instruction in the [Trace Compass Development Environment Setup][tc-dev-setup] guide of the `Trace Compass parent project`.

Note: Use the target file `tracecompass-incubator-master.target` under directory `common/org.eclipse.tracecompass.incubator.target` instead of the parent project's target file.

The sources section contains the git repository for the incubator. Make sure to clone it and add the plugins to the workspace.

In the source tree, there is a helper script that will create the necessary plugins for a new feature, including feature, ui, unit tests and documentation plugins.

```bash
cd org.eclipse.tracecompass.incubator
./skeleton/create_new_feature.py -h  # This will display the help message for the script with the available arguments
./skeleton/create_new_feature.py --dir tracetypes --copyright "École Polytechnique de Montréal" "My Awesome Tracetype"
```

This last command will create 6 plugins in directory ''tracetypes'' named ''org.eclipse.tracecompass.incubator.my.awesome.tracetype'' with the Activators initialized, the pom.xml files already populated, etc. You are now ready to import those plugins in Eclipse and start developing.

## Contributing a feature/patch

Read the [documentation on how to contribute](tc-contrib) of the Eclipse Trace Compass parent project to prepare to push your contribution as pull request.

This will send the patch to GitHub pull request, where it will be timely reviewed by a member of the community.

### Review guidelines

For new features, the review of two committers is necessary. The patch will not be reviewed in details. Some comments may be done on the code style, but they are not in themselves reasons for rejection of a patch. What will be verified:

- Does the feature work and integrate well with Trace Compass
- Does it seem stable enough (no obvious race conditions or UI freeze when simply playing around)
- Is there a minimal documentation to help the reviewer test and evaluate the feature. If adding a new trace type or analysis without providing a link to a trace to test it on, it will be hard to review.
- Are there some unit tests. Though not mandatory for a first pass on the feature, it is highly advised to have some basic unit tests, to help the maintainability

### Incubator committers

Since the incubator is meant to receive a lot of patches for different domains/trace types, not all committers are expected to know/maintain the full code base. Someone contributing a new feature will thus gain committers status quite rapidly once the feature is merged and is expected to be the official maintainer of that part of the code, hence should do the reviews on those plugins or at least make sure that the code does not go unmaintained and patches slip in the cracks.

## Publishing a feature

The script mentioned in the first section will create the plugins, but it will not publish it on the incubator update site. Some additional steps are required.

### Prepare the feature

First the feature plugins needs to be made ready with the proper dependencies, especially if some of those are themselves part of the incubator. The feature plugin is the one with no suffix. In the example above it would be ''org.eclipse.tracecompass.incubator.my.awesome.tracetype''.

- Open the ''feature.xml'' file in that plugin to open the feature editor.
  - In the ''Included plugins'' tab, make sure all the plugins that will be part of the feature are there. They are usually the .core, .ui and .doc.user plugins, the unit tests are not included.
  - In the ''Dependencies'' tab, click on the ''Compute'' button next to the list box on the left. This will list all the plugins that are necessary for the plugins of this feature to work.
- Open the ''feature.properties'' file
  - Edit the properties as you want to see them in the update site

### Add it to the update site

The project containing the update site content is ''org.eclipse.tracecompass.incubator.releng-site''.

- Open the ''category.xml'' file
  - You can now add your feature under the corresponding category or create a new category if necessary.

## UX concerns

Some minor things to remember when making new UIs is to remember that a human needs to operate them. Here are some minor guidelines to help out:

- The amount of time needed to click an item is directly proportional to the distance of the item and inversely proportional to the size of the item. So, making a user hunt for an item like "Where's Waldo(tm)" will typically decrease engagement. (Fitt's law)
- The amount of notions a user can juggle at once is typically seven, plus or minus two. This means a view should not have more than 5 metrics shown at once. (Miller's law)
- People will "fill in the blanks" so we half create what we perceive. This (Behavioural economics)
- Have fun and be creative, we want to incubate great new ideas.

## Note for maintainers

To make sure that the current incubator is working with a Trace Compass mainline release read the infomration [here][maintainer-info].

## Building the application

In order to build the application from command-line, Maven is used. For more information about building the application see the description in the [BUILDING](BUILDING.md) file.

## When to submit patches

Remember that contributions are always welcome!

If you have a simple and straightforward fix to an obvious bug, feel free to push it directly to the project's GitHub (see below).

This project uses GitHub issues to track ongoing development and issues. In order to contribute, please first [open an issue][issues] that clearly describes the bug you intend to fix or the feature you would like to add. Make sure you provide a way to reproduce the bug or test the proposed feature.

If you wish to work on a larger problem or feature, it would be a good idea to [contact us](#contact) first. It could avoid duplicate work in case somebody is already working on the same thing. For substantial new features, it is always good to discuss their design and integration first.

Be sure to search for existing bugs before you create another one.

Note that before migrating to GitHub issues ongoing development and issues were tracked using Bugzilla. The bugs were not batch migrated. Only selected issues have been migrated to GitHub issues. You can search for [here][bugzilla] for Bugzilla issues.

## Where to submit

The Trace Compass project uses GitHub pull requests for submitting patches and review contributions

Once you have your code ready for review, please  [open a pull request][pull-requests]. Please follow the [pull request guidelines][pr-guide].

## Pull request guidelines

**Changes to the project** are made by submitting code with a pull request (PR).

- [How to write and submit changes][creating-changes]

**Good commit messages** make it easier to review code and understand why the changes were made.
Please include a:

- `Title:` Concise and complete title written in imperative (e.g. "Update Gitpod demo screenshots"
or "Single-click to select or open trace")
- `Problem:` What is the situation that needs to be resolved? Why does the problem need fixing?
Link to related issues (e.g. "Fixes #317").
- `Solution:` What changes were made to resolve the situation? Why are these changes the right fix?
- `Impact:` What impact do these changes have? (e.g. Numbers to show a performance improvement,
screenshots or a video for a UI change)
- [*Sign-off:*][sign-off] Use your full name and a long-term email address. This certifies that you have written the code and that, from a licensing perspective, the code is appropriate for use in open-source.

## Contact

Contact the project developers via the [project's "dev" mailing list][mailing-list] or open an [issue tracker][issues].

[bugzilla]: https://bugs.eclipse.org/bugs/buglist.cgi?product=Tracecompass.incubator
[issues]: https://github.com/eclipse-tracecompass/org.eclipse.tracecompass.incubator/issues
[code-of-conduct]: https://github.com/eclipse-tracecompass/org.eclipse.tracecompass.incubator/blob/master/CODE_OF_CONDUCT.md
[commiter-handbook]: https://www.eclipse.org/projects/handbook/#resources-commit
[creating-changes]: https://www.dataschool.io/how-to-contribute-on-github/
[dev-process]: https://eclipse.org/projects/dev_process
[eca]: http://www.eclipse.org/legal/ECA.php
[ip-policy]: https://www.eclipse.org/org/documents/Eclipse_IP_Policy.pdf
[mailing-list]: https://dev.eclipse.org/mailman/listinfo/tracecompass-dev
[maintainer-info]: https://github.com/eclipse-tracecompass/org.eclipse.tracecompass/wiki/Align_to_tc_releases
[pr-guide]: #pull-request-guidelines
[pull-requests]: https://github.com/eclipse-tracecompass/org.eclipse.tracecompass/pulls
[sign-off]: https://git-scm.com/docs/git-commit#Documentation/git-commit.txt---signoff
[tc-dev-setup]: https://github.com/eclipse-tracecompass/org.eclipse.tracecompass/blob/master/DEVELOPMENT_ENV_SETUP.md
[terms]: https://www.eclipse.org/legal/termsofuse.php
