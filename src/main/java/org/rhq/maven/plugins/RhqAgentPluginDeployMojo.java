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

package org.rhq.maven.plugins;

import java.io.File;
import java.io.IOException;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.codehaus.plexus.util.FileUtils;

/**
 * Deploy a freshly built RHQ Agent Plugin to an RHQ container.
 *
 * @author Thomas Segismont
 */
@Mojo(name = "deploy", defaultPhase = LifecyclePhase.PACKAGE, threadSafe = true)
public class RhqAgentPluginDeployMojo extends AbstractMojo {

    /**
     * The build directory (root of build works)
     */
    @Parameter(defaultValue = "${project.build.directory}", required = true, readonly = true)
    private File buildDirectory;

    /**
     * The name of the generated RHQ agent plugin archive
     */
    @Parameter(defaultValue = "${project.build.finalName}", required = true, readonly = true)
    private String finalName;

    /**
     * The directory where the local RHQ development server pickups plugin archives.
     */
    @Parameter(required = true)
    private File deployDirectory;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        File agentPluginArchive = RhqAgentPluginMojo.getAgentPluginArchiveFile(buildDirectory, finalName);
        if (!agentPluginArchive.exists() && agentPluginArchive.isFile()) {
            throw new MojoExecutionException("Agent plugin archive does not exist: " + agentPluginArchive);
        }
        if (deployDirectory == null || !deployDirectory.exists() || !deployDirectory.isDirectory()) {
            throw new MojoExecutionException("Invalid deploy directory: " + String.valueOf(deployDirectory));
        }
        if (!deployDirectory.canWrite()) {
            throw new MojoExecutionException("No permission to write to " + deployDirectory);
        }
        try {
            // Copy plugin archive to the plugins directory of a local RHQ server
            FileUtils.copyFileToDirectory(agentPluginArchive, deployDirectory);
            getLog().info("Copied " + agentPluginArchive + " to " + deployDirectory);
        } catch (IOException e) {
            throw new MojoExecutionException("Failed to copy " + agentPluginArchive + " to " + deployDirectory);
        }
    }

}
