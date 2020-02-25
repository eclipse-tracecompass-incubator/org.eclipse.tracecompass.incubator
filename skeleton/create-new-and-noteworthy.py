#!/usr/bin/env python3
###############################################################################
# Copyright (c) 2019 Ericsson
#
# All rights reserved. This program and the accompanying materials
# are made available under the terms of the Eclipse Public License 2.0
# which accompanies this distribution, and is available at
# https://www.eclipse.org/legal/epl-2.0
#
# SPDX-License-Identifier: EPL-2.0
###############################################################################

import io
import subprocess
import sys
import argparse

report = dict()

parser = argparse.ArgumentParser(description='Generates a new and noteworthy in markdown from a git tree using two dates (yyyy-MM-dd) or commit ids.')
parser.add_argument('-a','--after',  help='Include commits after and including this specific date or SHA1', required=True)
parser.add_argument('-b','--before', help='Include commits before and including this specific date or SHA1', required=True)

args = parser.parse_args()

def update_entry(entry, line):
    if line.lower().startswith(entry):
        if entry not in report:
             report[entry] = list()
        report[entry].append(line[len(entry):].strip())

def update_report(line):
    update_entry("[added]", line)
    update_entry("[removed]", line)
    update_entry("[fixed]", line)
    update_entry("[deprecated]", line)
    update_entry("[security]", line)
    update_entry("[changed]", line)

if __name__=='__main__':
    after = args.after
    before = args.before
    cmd = ['git', '--no-pager','log', '--after', after, '--until', before]
    proc = subprocess.Popen(cmd, stdout=subprocess.PIPE)
    commit =""
    for line in io.TextIOWrapper(proc.stdout, encoding="utf-8"):
        try:
            line = line.strip()
            if (line.startswith("commit")):
                commit = line[len("commit "):].strip()
            update_report(line)
        except UnicodeDecodeError as e:
            print ("Error {0} in {1}, could not parse commit message {2}".format(e, commit, line))
    print ("# New and Noteworthy for {0} to {1}.".format(after, before))
    for entry in report:
        print ("\n")
        print ("## " + entry.title())
        for line in report[entry]:
             print(line)