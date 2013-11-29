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
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.io.IOUtils;
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
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.StringUtils;

/**
 * Validates a freshly built RHQ Agent Plugin.
 *
 * @author Thomas Segismont
 */
@Mojo(name = "validate", defaultPhase = LifecyclePhase.PACKAGE, threadSafe = true)
public class ValidateMojo extends AbstractMojo {

    private static final String PLUGIN_VALIDATOR_MODULE_GROUP_ID = "org.rhq";
    private static final String PLUGIN_VALIDATOR_MODULE_ARTIFACT_ID = "rhq-core-plugin-container";
    private static final String VALIDATOR_LOG4J_PROPERTIES = "validator-log4j.properties";
    private static final String PLUGIN_VALIDATOR_MAIN_CLASS = "org.rhq.core.pc.plugin.PluginValidator";
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
     * Version of the RHQ Plugin Container API
     */
    @Parameter(required = true)
    private String rhqVersion;

    /**
     * Whether to fail the build if an error occurs while validating the plugin.
     */
    @Parameter(defaultValue = "true")
    private boolean failOnError;

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
        File agentPluginArchive = PackageMojo.getAgentPluginArchiveFile(buildDirectory, finalName);
        if (!agentPluginArchive.exists() && agentPluginArchive.isFile()) {
            throw new MojoExecutionException("Agent plugin archive does not exist: " + agentPluginArchive);
        }
        validate(agentPluginArchive);
    }

    private void validate(File agentPluginArchive) throws MojoExecutionException {
        // Run the plugin validator as a forked process
        Process process = null;
        try {
            String pluginValidatorClasspath = buildPluginValidatorClasspath();
            String javaCommand = buildJavaCommand(pluginValidatorClasspath);
            if (getLog().isDebugEnabled()) {
                getLog().debug(IOUtils.LINE_SEPARATOR + "pluginValidatorClasspath = " + pluginValidatorClasspath +
                        IOUtils.LINE_SEPARATOR + IOUtils.LINE_SEPARATOR + "javaCommand = " + javaCommand);
            }
            ProcessBuilder processBuilder = buildProcessBuilder(agentPluginArchive, pluginValidatorClasspath,
                    javaCommand);
            process = processBuilder.start();
            redirectOuput(process);
            int exitCode = process.waitFor();
            if (exitCode != 0) {
                handleFailure("Invalid plugin");
            }
        } catch (Exception e) {
            handleException(e);
        } finally {
            if (process != null) {
                process.destroy();
            }
        }
    }

    private String buildPluginValidatorClasspath() throws ArtifactNotFoundException, ArtifactResolutionException,
            IOException {
        // Resolve dependencies of the plugin validator module
        Artifact dummyOriginatingArtifact =
                artifactFactory.createBuildArtifact("org.apache.maven.plugins", "maven-downloader-plugin", "1.0",
                        "jar");
        Artifact pluginContainerArtifact = this.artifactFactory.createArtifact(
                PLUGIN_VALIDATOR_MODULE_GROUP_ID, PLUGIN_VALIDATOR_MODULE_ARTIFACT_ID, rhqVersion,
                null, "jar");
        ArtifactResolutionResult artifactResolutionResult = artifactResolver.resolveTransitively(Collections
                .singleton(pluginContainerArtifact), dummyOriginatingArtifact, localRepository,
                remoteRepositories, artifactMetadataSource, null);

        List<String> classpathElements = new LinkedList<String>();

        // Add all compile scope dependencies to the classpath
        Iterator iterator = artifactResolutionResult.getArtifacts().iterator();
        ScopeArtifactFilter artifactFilter = new ScopeArtifactFilter(Artifact.SCOPE_COMPILE);
        while (iterator.hasNext()) {
            Artifact artifact = (Artifact) iterator.next();
            if (!artifact.isOptional() && artifact.getType().equals("jar") && artifactFilter.include(artifact)) {
                classpathElements.add(artifact.getFile().getAbsolutePath());
            }
        }

        // The plugin validator module does not provide a log4 configuration file so we provide one
        File directory = new File(buildDirectory, "validator-log4j");
        if (!directory.exists()) {
            directory.mkdir();
        }
        classpathElements.add(directory.getAbsolutePath());
        File log4jFile = new File(directory, VALIDATOR_LOG4J_PROPERTIES);
        FileOutputStream outputStream = null;
        InputStream inputStream = getClass().getClassLoader().getResourceAsStream(VALIDATOR_LOG4J_PROPERTIES);
        try {
            outputStream = new FileOutputStream(log4jFile);
            IOUtil.copy(inputStream, outputStream);
        } finally {
            IOUtil.close(inputStream);
            IOUtil.close(outputStream);
        }

        return StringUtils.join(classpathElements.iterator(), File.pathSeparator);
    }

    private String buildJavaCommand(String pluginValidatorClasspath) {
        String javaHome = System.getProperty("java.home");
        return javaHome + File.separator + "bin" + File.separator + "java";
    }

    private ProcessBuilder buildProcessBuilder(File agentPluginArchive, String pluginValidatorClasspath,
                                               String javaCommand) {
        return new ProcessBuilder() //
                .directory(buildDirectory) //
                .command(javaCommand, //
                        "-classpath", //
                        pluginValidatorClasspath, //
                        "-Dlog4j.configuration=" + VALIDATOR_LOG4J_PROPERTIES, //
                        PLUGIN_VALIDATOR_MAIN_CLASS, //
                        agentPluginArchive.getAbsolutePath());
    }

    private void redirectOuput(Process process) {
        startCopyThread(process.getInputStream(), System.out);
        startCopyThread(process.getErrorStream(), System.err);
    }

    private void startCopyThread(final InputStream inputStream, final PrintStream printStream) {
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    IOUtil.copy(inputStream, printStream);
                } catch (IOException ignore) {
                }
            }
        });
        thread.setDaemon(true);
        thread.start();
    }

    private void handleFailure(String message) throws MojoFailureException {
        if (failOnError) {
            throw new MojoFailureException(message);
        }
        getLog().error(message);
    }

    private void handleException(Exception e) throws MojoExecutionException {
        if (failOnError) {
            throw new MojoExecutionException(e.getMessage(), e);
        }
        getLog().error(e.getMessage(), e);
    }
}
