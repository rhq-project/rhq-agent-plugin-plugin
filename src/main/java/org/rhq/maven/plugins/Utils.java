/*
 * RHQ Management Platform
 * Copyright 2014, Red Hat Middleware LLC, and individual contributors
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

package org.rhq.maven.plugins;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.resolver.filter.ScopeArtifactFilter;
import org.apache.maven.project.MavenProject;
import org.zeroturnaround.zip.ZipUtil;

/**
 * @author Thomas Segismont
 */
public class Utils {

    public static File getAgentPluginArchiveFile(File buildDirectory, String finalName) {
        return new File(buildDirectory, finalName + ".jar");
    }

    public static Set<File> findParentPlugins(MavenProject agentPluginProject) throws IOException {
        Set<File> parentPlugins = new HashSet<File>();
        Iterator projectArtifacts = agentPluginProject.getArtifacts().iterator();
        ScopeArtifactFilter artifactFilter = new ScopeArtifactFilter(Artifact.SCOPE_COMPILE);
        while (projectArtifacts.hasNext()) {
            Artifact artifact = (Artifact) projectArtifacts.next();
            if (!artifact.isOptional() && artifact.getType().equals("jar") && artifactFilter.include(artifact)) {
                File artifactFile = artifact.getFile();
                if (isAgentPlugin(artifactFile)) {
                    parentPlugins.add(artifactFile);
                }
            }
        }
        return parentPlugins;
    }

    public static boolean isAgentPlugin(File jarFile) {
        return ZipUtil.containsEntry(jarFile, "META-INF/rhq-plugin.xml");
    }

    public static void redirectInput(Process process) {
        startCopyThread(System.in, process.getOutputStream());
    }

    public static void redirectOuput(Process process) {
        startCopyThread(process.getInputStream(), System.out);
        startCopyThread(process.getErrorStream(), System.err);
    }

    public static void startCopyThread(final InputStream inputStream, final OutputStream outputStream) {
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    byte[] buffer = new byte[1024];
                    for (int n = inputStream.read(buffer); n != -1; n = inputStream.read(buffer)) {
                        outputStream.write(buffer, 0, n);
                        outputStream.flush();
                    }
                } catch (IOException ignore) {
                }
            }
        });
        thread.setDaemon(true);
        thread.start();
    }
}
