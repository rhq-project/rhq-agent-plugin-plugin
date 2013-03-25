/*
 * RHQ Management Platform
 * Copyright 2013, Red Hat Middleware LLC, and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */

package org.rhq.maven.plugins;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;

import org.apache.maven.archiver.MavenArchiveConfiguration;
import org.apache.maven.archiver.MavenArchiver;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.resolver.filter.ScopeArtifactFilter;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.archiver.Archiver;
import org.codehaus.plexus.archiver.jar.JarArchiver;
import org.codehaus.plexus.util.FileUtils;

/**
 * Packager for an RHQ Agent Plugin.
 * 
 * @author Thomas Segismont
 */
@Mojo(name = "rhq-agent-plugin", defaultPhase = LifecyclePhase.PACKAGE, requiresDependencyResolution = ResolutionScope.RUNTIME, threadSafe = true)
public class RhqAgentPluginMojo extends AbstractMojo {

    /**
     * The build directory (root of build works)
     */
    @Parameter(defaultValue = "${project.build.directory}", required = true, readonly = true)
    private File buildDirectory;

    /**
     * The output directory (where standard plugins put compiled classes and resources)
     */
    @Parameter(defaultValue = "${project.build.outputDirectory}", required = true, readonly = true)
    private File outputDirectory;

    /**
     * The lib directory (where standard plugins put compiled classes and resources)
     */
    @Parameter(defaultValue = "${project.build.directory}/lib", required = true)
    private File libDirectory;

    /**
     * The name of the generated RHQ agent plugin archive
     */
    @Parameter(defaultValue = "${project.build.finalName}", required = true, readonly = true)
    private String finalName;

    /**
     * This will allow to get our plugin configured like any archiver plugin
     * 
     * See <a href="http://maven.apache.org/shared/maven-archiver/index.html">Maven Archiver Reference</a>.
     */
    @Parameter
    private MavenArchiveConfiguration archive = new MavenArchiveConfiguration();

    @Component(role = Archiver.class, hint = "jar")
    private JarArchiver jarArchiver;

    @Component
    private MavenProject project;

    @Component
    private MavenSession session;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        // Create the package and set it as the main project artifact
        project.getArtifact().setFile(createAgentPluginArchive());
    }

    private File createAgentPluginArchive() throws MojoExecutionException {

        // Create the Java IO File denoting the project package
        File agentPluginArchive = getAgentPluginArchiveFile(buildDirectory, finalName);
        if (getLog().isDebugEnabled()) {
            getLog().debug("Starting packaging of the plugin to file " + agentPluginArchive);
        }

        // Configure the Maven archiver to use JAR archive utility 
        MavenArchiver archiver = new MavenArchiver();
        archiver.setArchiver(jarArchiver);
        archiver.setOutputFile(agentPluginArchive);

        if (libDirectory.exists()) {
            // Clean the lib working directory
            try {
                FileUtils.forceDelete(libDirectory);
            } catch (IOException e) {
                throw new MojoExecutionException("Unable to delete " + libDirectory, e);
            }
        }

        try {

            // Request compiled classes to be added to the archive
            // TODO : manage includes and excludes
            archiver.getArchiver().addDirectory(outputDirectory);

            // Now request JAR dependencies of scope runtime to get included
            // This call to #getArtifacts only works because the mojo requires dependency resolution of scope RUNTIME
            Iterator projectArtifacts = project.getArtifacts().iterator();
            ScopeArtifactFilter artifactFilter = new ScopeArtifactFilter(Artifact.SCOPE_RUNTIME);
            while (projectArtifacts.hasNext()) {
                Artifact artifact = (Artifact) projectArtifacts.next();
                if (getLog().isDebugEnabled()) {
                    getLog().info("Found project artifact: " + artifact);
                }
                if (!artifact.isOptional() && artifact.getType().equals("jar") && artifactFilter.include(artifact)) {
                    if (getLog().isDebugEnabled()) {
                        getLog().info("Will add " + artifact + " to the plugin archive");
                    }
                    FileUtils.copyFileToDirectory(artifact.getFile(), libDirectory);
                }
            }
            // This directory will exist only if at least one dependency was added
            if (libDirectory.exists()) {
                // Request all found runtime dependencies to be added to the archive under the 'lib' directory
                archiver.getArchiver().addDirectory(libDirectory, "lib/");
            }

            archiver.createArchive(session, project, archive);

        } catch (Exception e) {
            throw new MojoExecutionException("Could not create agent plugin archive", e);
        }

        return agentPluginArchive;
    }

    static File getAgentPluginArchiveFile(File buildDirectory, String finalName) {
        return new File(buildDirectory, finalName + ".jar");
    }

}
