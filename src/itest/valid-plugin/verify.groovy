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



import java.util.jar.JarFile

def pluginFile = new File(basedir, "target/itest-valid-plugin-1.0-SNAPSHOT.jar")
assert pluginFile.isFile() : pluginFile + " is not a file"

JarFile pluginJarFile = new JarFile(pluginFile)
assert pluginJarFile.getJarEntry("lib/commons-codec-1.3.jar") != null: "commons-codec library not found"
assert pluginJarFile.getJarEntry("lib/commons-httpclient-3.1.jar") != null: "commons-httpclient not found"
