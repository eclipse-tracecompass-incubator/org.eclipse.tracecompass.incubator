#!/usr/bin/env python3
###############################################################################
# Copyright (c) 2017 École Polytechnique de Montréal
#
# All rights reserved. This program and the accompanying materials
# are made available under the terms of the Eclipse Public License v1.0
# which accompanies this distribution, and is available at
# http://www.eclipse.org/legal/epl-v10.html
###############################################################################

import argparse
import shutil
import os

parser = argparse.ArgumentParser(description='Creates the plugins and feature for a new incubator functionnality.')
parser.add_argument('name', help='The human readable name of the plugins and feature to add. The plugin names will the the dot-separated lowercase name. For example if name is "My Test Plugin", plugins will be named org.eclipse.tracecompass.incubator.my.test.plugin')
parser.add_argument('--dir', help='Directory in which to add the plugins')
parser.add_argument('--no-ui', dest='noUi', action='store_const', const=True, default=False, help='Whether to add a UI plugin for this feature')
parser.add_argument('--no-help', dest='noHelp', action='store_const', const=True, default=False, help='Whether to add an help plugin for this feature')
parser.add_argument('--copyright', default="École Polytechnique de Montréal", help='The organisation that has the copyright on the new files')

args = parser.parse_args()
idPlaceholder = "{%skeleton}"
namePlaceholder = "{%skeletonName}"
copyrightPlaceholder = "{%copyright}"
copyright = args.copyright

baseDir = os.path.dirname(os.path.realpath(__file__))

featurePluginStrPlaceholder = "<!-- insert plugins here -->"
featurePluginStr = """ 
   <plugin
         id="{%plugin}"
         download-size="0"
         install-size="0"
         version="0.0.0"
         unpack="false"/> 
"""

pomModulePlaceholder = "<!-- insert modules here -->"
pomModuleStr = """<module>org.eclipse.tracecompass.incubator.{%skeleton}{%suffix}</module>
    """

def copyAndUpdate(srcDir, destDir, name, id):
    shutil.copytree(srcDir, destDir)
    shutil.move(destDir + '/.project.skel', destDir + '/.project')
    for dname, dirs, files in os.walk(destDir):
        for fname in files:
            fpath = os.path.join(dname, fname)
            print(fpath)
            try:
                with open(fpath, encoding = "utf-8") as f:
                    s = f.read()
                s = s.replace(idPlaceholder, id)
                s = s.replace(namePlaceholder, name)
                s = s.replace(copyrightPlaceholder, copyright)
                with open(fpath, encoding = "utf-8", mode = "w") as f:
                    f.write(s)
            except ValueError:
                print("Problem opening file. That may be normal if the file is not a text file")

def moveActivator(moveTo, suffix, id):
    os.makedirs(moveTo + '.' + suffix + '/src/org/eclipse/tracecompass/incubator/internal/' + id.replace('.', '/') + '/' + suffix)
    shutil.move(moveTo + '.' + suffix + '/src/Activator.java', moveTo + '.' + suffix + '/src/org/eclipse/tracecompass/incubator/internal/' + id.replace('.', '/') + '/' + suffix)
    shutil.move(moveTo + '.' + suffix + '/src/package-info.java', moveTo + '.' + suffix + '/src/org/eclipse/tracecompass/incubator/internal/' + id.replace('.', '/') + '/' + suffix)

def moveActivatorTest(moveTo, suffix, id):
    os.makedirs(moveTo + '.' + suffix + '/src/org/eclipse/tracecompass/incubator/' + id.replace('.', '/') + '/' + suffix.replace('.', '/'))
    shutil.move(moveTo + '.' + suffix + '/src/ActivatorTest.java', moveTo + '.' + suffix + '/src/org/eclipse/tracecompass/incubator/' + id.replace('.', '/') + '/' + suffix.replace('.', '/'))

def updatePom(baseDir, destDir, id, moduleStr):
    # Does a pom.xml exists in the destination directory?
    destPom = destDir + "/pom.xml"
    if not os.path.isfile(destDir + "/pom.xml"):
        shutil.copyfile(baseDir + "/pom.xml", destPom)
        
    with open(destPom, encoding = "utf-8") as f:
        s = f.read();
    s = s.replace("{%dir}", destDir)
    s = s.replace(pomModulePlaceholder, moduleStr + pomModulePlaceholder)
    s = s.replace(copyrightPlaceholder, copyright)
    with open(destPom, encoding = "utf-8", mode = 'w+') as f:
        f.write(s)
    

def copyDirs(fullname, dir, noUi, noHelp):
    if dir is None:
        dir = '.'

    id = fullname.lower().replace(' ', '.')
    pomModule = ""
    moveTo = dir + '/org.eclipse.tracecompass.incubator.' + id
    print('Copying skeleton directories to ' + moveTo + '[.*]')
    copyAndUpdate(baseDir + '/skeleton.feature', moveTo, fullname, id)
    pomModule = pomModuleStr.replace(idPlaceholder, id).replace("{%suffix}", "")
    copyAndUpdate(baseDir + '/skeleton.core', moveTo + '.core', fullname, id)
    moveActivator(moveTo, "core", id)
    pluginStr = featurePluginStr.replace("{%plugin}", "org.eclipse.tracecompass.incubator." + id + ".core")
    pomModule += pomModuleStr.replace(idPlaceholder, id).replace("{%suffix}", ".core")
    
    copyAndUpdate(baseDir + '/skeleton.core.tests', moveTo + '.core.tests', fullname, id)
    moveActivatorTest(moveTo, "core.tests", id)
    pomModule += pomModuleStr.replace(idPlaceholder, id).replace("{%suffix}", ".core.tests")

    if not(noUi):
        copyAndUpdate(baseDir + '/skeleton.ui', moveTo + '.ui', fullname, id)
        moveActivator(moveTo, "ui", id)
        pluginStr = pluginStr + featurePluginStr.replace("{%plugin}", "org.eclipse.tracecompass.incubator." + id + ".ui")
        pomModule += pomModuleStr.replace(idPlaceholder, id).replace("{%suffix}", ".ui")
        copyAndUpdate(baseDir + '/skeleton.ui.swtbot.tests', moveTo + '.ui.swtbot.tests', fullname, id)
        os.makedirs(moveTo + '.ui.swtbot.tests/src')

    if not(noHelp):
        copyAndUpdate(baseDir + '/skeleton.doc.user', baseDir + '/../doc/org.eclipse.tracecompass.incubator.' + id + '.doc.user', fullname, id)
        pluginStr = pluginStr + featurePluginStr.replace("{%plugin}", "org.eclipse.tracecompass.incubator." + id + ".doc.user")
        # Update the pom.xml
        updatePom(baseDir, baseDir + '/../doc', id, pomModuleStr.replace(idPlaceholder, id).replace("{%suffix}", ".doc.user"))

    # Add the appropriate plugins to the feature.xml
    fpath = os.path.join(moveTo, "feature.xml")
    with open(fpath, encoding = "utf-8") as f:
        s = f.read()
    s = s.replace(featurePluginStrPlaceholder, pluginStr)
    with open(fpath, "w") as f:
        f.write(s)

    # Update the pom.xml if necessary
    updatePom(baseDir, dir, id, pomModule)

    print('------------------------------')
    print('Congratulations! Your new plugins are ready to be populated and add magnificent features to Trace Compass!')
    print("")
    print("For the Hudson jobs to take them in, don't forget the add them to the appropriate pom.xml files and if necessary, create a pom.xml file in the parent directory. A pom.xml file may have been created or updated in the install directory, but the unit tests plugin need to be added manually when tests are available.")

copyDirs(args.name, args.dir, args.noUi, args.noHelp)


