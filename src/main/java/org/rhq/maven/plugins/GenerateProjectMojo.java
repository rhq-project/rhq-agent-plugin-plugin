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
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.metadata.ArtifactMetadataSource;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.ArtifactNotFoundException;
import org.apache.maven.artifact.resolver.ArtifactResolutionException;
import org.apache.maven.artifact.resolver.ArtifactResolutionResult;
import org.apache.maven.artifact.resolver.ArtifactResolver;
import org.apache.maven.artifact.resolver.filter.ScopeArtifactFilter;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import static org.codehaus.plexus.util.StringUtils.join;
import static org.rhq.maven.plugins.Utils.redirectInput;
import static org.rhq.maven.plugins.Utils.redirectOuput;

/**
 * @author Thomas Segismont
 */
@Mojo(name = "generateProject", requiresProject = false)
public class GenerateProjectMojo extends AbstractMojo {

    private static final String PLUGIN_GENERATOR_MODULE_GROUP_ID = "org.rhq.helpers";
    private static final String PLUGIN_GENERATOR_MODULE_ARTIFACT_ID = "rhq-pluginGen";
    private static final String PLUGIN_GENERATOR_MAIN_CLASS = "org.rhq.helpers.pluginGen.PluginGen";
    //TODO expose as attribute
    private static final String PLUGIN_GENERATOR_VERSION = "3.0.4";

    @Parameter(defaultValue = "${project.remoteArtifactRepositories}", required = true, readonly = true)
    private List remoteRepositories;

    @Parameter(defaultValue = "${localRepository}", required = true, readonly = true)
    private ArtifactRepository localRepository;

    @Component
    private ArtifactFactory artifactFactory;

    @Component
    private ArtifactResolver artifactResolver;

    @Component
    private ArtifactMetadataSource artifactMetadataSource;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        Process process = null;
        try {
            String generatorClasspath = buildGeneratorClasspath();
            String javaCommand = buildJavaCommand();
            ProcessBuilder processBuilder = buildProcessBuilder(javaCommand,
                    generatorClasspath);
            process = processBuilder.start();
            redirectInput(process);
            redirectOuput(process);
            int exitCode = process.waitFor();
            if (exitCode != 0) {
                throw new MojoFailureException("Process terminated with exitCode=" + exitCode);
            }
        } catch (Exception e) {
            throw new MojoFailureException("Exception caught", e);
        } finally {
            if (process != null) {
                process.destroy();
            }
        }
    }

    private String buildGeneratorClasspath() throws ArtifactNotFoundException,
            ArtifactResolutionException, IOException {
        // Resolve dependencies of the plugin validator module
        Artifact dummyOriginatingArtifact = artifactFactory
                .createBuildArtifact("org.apache.maven.plugins",
                        "maven-downloader-plugin", "1.0", "jar");
        Artifact pluginContainerArtifact = this.artifactFactory.createArtifact(
                PLUGIN_GENERATOR_MODULE_GROUP_ID,
                PLUGIN_GENERATOR_MODULE_ARTIFACT_ID, PLUGIN_GENERATOR_VERSION, null, "jar");
        ArtifactResolutionResult artifactResolutionResult = artifactResolver
                .resolveTransitively(
                        Collections.singleton(pluginContainerArtifact),
                        dummyOriginatingArtifact, localRepository,
                        remoteRepositories, artifactMetadataSource, null);

        List<String> classpathElements = new LinkedList<String>();

        // Add all compile scope dependencies to the classpath
        Iterator iterator = artifactResolutionResult.getArtifacts().iterator();
        ScopeArtifactFilter artifactFilter = new ScopeArtifactFilter(
                Artifact.SCOPE_COMPILE);
        while (iterator.hasNext()) {
            Artifact artifact = (Artifact) iterator.next();
            if (!artifact.isOptional() && artifact.getType().equals("jar")
                    && artifactFilter.include(artifact)) {
                classpathElements.add(artifact.getFile().getAbsolutePath());
            }
        }

        String pluginGeneratorClasspath = join(classpathElements.iterator(),
                File.pathSeparator);

        if (getLog().isDebugEnabled()) {
            getLog().debug(
                    "pluginGeneratorClasspath = " + pluginGeneratorClasspath);
        }

        return pluginGeneratorClasspath;
    }

    private String buildJavaCommand() {
        String javaHome = System.getProperty("java.home");
        String javaCommand = javaHome + File.separator + "bin" + File.separator
                + "java";
        if (getLog().isDebugEnabled()) {
            getLog().debug("javaCommand = " + javaCommand);
        }
        return javaCommand;
    }

    private ProcessBuilder buildProcessBuilder(String javaCommand,
                                               String pluginGeneratorClasspath) {
        List<String> command = new LinkedList<String>();
        command.add(javaCommand);
        command.add("-classpath");
        command.add(pluginGeneratorClasspath);
        command.add("-Dorg.apache.commons.logging.Log=org.apache.commons.logging.impl.SimpleLog");
        command.add(PLUGIN_GENERATOR_MAIN_CLASS);
        if (getLog().isDebugEnabled()) {
            getLog().debug(
                    "Built command to execute = "
                            + join(command.iterator(), " ")
            );
        }
        return new ProcessBuilder().directory(new File(System.getProperty("user.dir"))).command(command);
    }
}
