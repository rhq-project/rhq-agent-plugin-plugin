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

def pluginsDir_1 = new File(basedir, "plugin1/target/itest/plugins")
def platformPluginFile_1 = new File(pluginsDir_1, "rhq-platform-plugin-4.9.0.jar")
assert platformPluginFile_1.isFile(): platformPluginFile_1 + " is not a file"
def agentPluginFile_1 = new File(pluginsDir_1, "plugin1-1.0-SNAPSHOT.jar")
assert agentPluginFile_1.isFile(): agentPluginFile_1 + " is not a file"
def jmxPluginFile_1 = new File(pluginsDir_1, "rhq-jmx-plugin-4.9.0.jar")
assert jmxPluginFile_1.isFile(): jmxPluginFile_1 + " is not a file"

def libDir_1 = new File(basedir, "plugin1/target/itest/lib")
assert libDir_1.isDirectory(): libDir_1 + " is not a directory"
def sigarFiles_1 = libDir_1.list()
assert "sigar.jar" in sigarFiles_1: "sigar.jar is not in the list of files: " + sigarFiles_1

def pluginsDir_2 = new File(basedir, "plugin2/target/itest/plugins")
def platformPluginFile_2 = new File(pluginsDir_2, "rhq-platform-plugin-4.9.0.jar")
assert platformPluginFile_2.isFile(): platformPluginFile_2 + " is not a file"
def agentPluginFile_2 = new File(pluginsDir_2, "plugin2-1.0-SNAPSHOT.jar")
assert agentPluginFile_2.isFile(): agentPluginFile_2 + " is not a file"
def jmxPluginFile_2 = new File(pluginsDir_2, "rhq-jmx-plugin-4.9.0.jar")
assert jmxPluginFile_2.isFile(): jmxPluginFile_2 + " is not a file"

def libDir_2 = new File(basedir, "plugin2/target/itest/lib")
assert libDir_2.isDirectory(): libDir_2 + " is not a directory"
def sigarFiles_2 = libDir_2.list()
assert "sigar.jar" in sigarFiles_2: "sigar.jar is not in the list of files: " + sigarFiles_2
