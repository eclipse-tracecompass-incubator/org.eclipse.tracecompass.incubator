Eclipse Trace Compass RCP Skeleton
==================================

Use this skeleton to create a custom RCP based on the Trace Compass RCP.

The skeleton includes some Trace Compass and Trace Compass incubator features and plug-ins. The list of features and plug-ins needs to be modified meet requirement of your custom RCP.

Notes:

* Creating a custom RCP based on Trace Compass is more involving than creating a Trace Compass plug-in extension.
* The Trace Compass incubator RCP is a good example for a custom RCP, and you can use it as reference.
* The current definition will build RCPs for Windows, Linux and MacOS. You can tweak the `pom.xml` files to remove unwanted target architectures.
* `skeleton.rcp` is tailored for Trace Compass 8.0.0 release. Using releases might require updates of the custom RCP definition.
* If build failures of mising dependencies occur due to added/removed Eclipse features and plug-ins, then update the target definition, RCP feature and/or product defintion accordingly.

To create a custom RCP do the following steps:

* Copy `skeleton.rcp/*` to a new location (e.g. your new git repository)
* Fix TODOs in different files
* Add custom Eclipse features (which are collection of Eclipse plug-ins):
  * In `skeleton.tracecompass.target/skeleton.tracecompass.target`: add dependency in target definition (if needed)
  * In `skeleton.tracecompass.rcp.product/skeleton.product`: add new feature ids
* Add selected Eclipse plug-ins:
  * In skeleton.tracecompass.target/skeleton.tracecompass.target:
    Add dependency in target defintion (if needed)
  * In `skeleton.tracecompass.rcp/feature.xml`: add new plug-ins
  * In `skeleton.tracecompass.rcp/pom.xml`: add new plug-ins (under excluded sources)
* Remove Eclipse features/plug-ins of the skeleton that are not needed/wanted for your custom RCP.
* Change launcher name and workspace
  * in `skeleton.tracecompass.rcp.product/skeleton.product`:
    * Replace string `skeleton-launcher` with your launcher name
    * Replace string `skeleton-ws` with your workspace name in the user's home directory
* Update branding icons ans splash screen
  * `skeleton.tracecompass.rcp.branding/splash.bmp`
  * `skeleton.tracecompass.rcp.branding/icons/*`
  * In 'skeleton.tracecompass.rcp.product/skeleton.product`: update for different icon file names (if changed)
* Change default Perspective (if needed)
  * In `skeleton.tracecompass.rcp.branding/plugin_customization.ini`: Replace `org.eclipse.linuxtools.tmf.ui.perspective` with the wanted perspective ID
* Replace the string `skeleton` everywhere:
  * Directory names
  * File names
  * In all the files of each sub-directory (including all .project files)
* Now you can also import the plug-ins to an Eclipse IDE

* To build:
  * In directory `skeleton.tracecompass.parent` type: `mvn clean install`
  * RCP output location: `../skeleton.tracecompass.rcp.product/target/products/`
