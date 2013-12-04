/*
 * RHQ Management Platform
 * Copyright 2013, Red Hat Middleware LLC, and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */




def pluginsDir = new File(basedir, "target/itest/plugins")
def platformPluginFile = new File(pluginsDir, "rhq-platform-plugin-4.9.0.jar")
assert platformPluginFile.isFile(): platformPluginFile + " is not a file"
def agentPluginFile = new File(pluginsDir, "setup-test-plugin-container-1.0-SNAPSHOT.jar")
assert agentPluginFile.isFile(): agentPluginFile + " is not a file"

def libDir = new File(basedir, "target/itest/lib")
assert libDir.isDirectory(): libDir + " is not a directory"
def sigarFiles = libDir.list()
assert "sigar.jar" in sigarFiles: "sigar.jar is not in the list of files: " + sigarFiles
