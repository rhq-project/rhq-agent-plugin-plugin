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
import java.io.InputStream;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.zip.ZipEntry;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.metadata.ArtifactMetadataSource;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.ArtifactResolutionResult;
import org.apache.maven.artifact.resolver.ArtifactResolver;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.io.RawInputStreamFacade;
import org.zeroturnaround.zip.ZipEntryCallback;
import org.zeroturnaround.zip.ZipUtil;

/**
 * Setup a directory with all files needed to start a test plugin container.
 *
 * @author Thomas Segismont
 */
@Mojo(name = "setup-test-plugin-container", defaultPhase = LifecyclePhase.PRE_INTEGRATION_TEST, threadSafe = true)
public class SetupTestPluginContainerMojo extends AbstractMojo {

    private static final String RHQ_PLATFORM_PLUGIN_GROUP_ID = "org.rhq";
    private static final String RHQ_PLATFORM_PLUGIN_ARTIFACT_ID = "rhq-platform-plugin";
    private static final String SIGAR_GROUP_ID = "org.hyperic";
    private static final String SIGAR_ARTIFACT_ID = "sigar-dist";

    /**
     * The build directory (root of build works).
     */
    @Parameter(defaultValue = "${project.build.directory}", required = true, readonly = true)
    protected File buildDirectory;

    /**
     * The name of the generated RHQ agent plugin archive
     */
    @Parameter(defaultValue = "${project.build.finalName}", required = true, readonly = true)
    private String finalName;

    /**
     * The CLI directory (where to install the RHQ CLI).
     */
    @Parameter(defaultValue = "${project.build.directory}/itest", required = true)
    protected File itestDirectory;

    /**
     * Version of the RHQ Platform Plugin.
     */
    @Parameter(required = true)
    protected String rhqVersion;

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
        deleteItestDirectoryIfExists();
        createItestDirectory();
        File pluginsDirectory = createChildDirectory("plugins");
        File libDirectory = createChildDirectory("lib");
        ArtifactResolutionResult platformPluginResolutionResult = resolvePlatformPluginArtifact();
        File platformPluginFile = findPlatformPluginFile(platformPluginResolutionResult);
        Set<File> requiredPlugins = new HashSet<File>();
        requiredPlugins.add(platformPluginFile);
        File agentPluginArchive = PackageMojo.getAgentPluginArchiveFile(buildDirectory, finalName);
        requiredPlugins.add(agentPluginArchive);
        copyRequiredPlugins(pluginsDirectory, requiredPlugins);
        File sigarDistributionFile = findSigarDistributionFile(platformPluginResolutionResult);
        copySigarLibs(libDirectory, sigarDistributionFile);
    }

    private void deleteItestDirectoryIfExists() throws MojoExecutionException {
        try {
            FileUtils.forceDelete(itestDirectory);
        } catch (IOException e) {
            throw new MojoExecutionException("Could not delete" + itestDirectory.getAbsolutePath(), e);
        }
    }

    private void createItestDirectory() throws MojoExecutionException {
        try {
            FileUtils.forceMkdir(itestDirectory);
        } catch (IOException e) {
            throw new MojoExecutionException("Could not create" + itestDirectory.getAbsolutePath(), e);
        }
    }

    private File createChildDirectory(String childDirectoryName) throws MojoExecutionException {
        File childDirectory = new File(itestDirectory, childDirectoryName);
        try {
            FileUtils.forceMkdir(childDirectory);
        } catch (IOException e) {
            throw new MojoExecutionException("Could not create child directory " + childDirectory.getAbsolutePath(), e);
        }
        return childDirectory;
    }

    private ArtifactResolutionResult resolvePlatformPluginArtifact() throws MojoExecutionException {
        Artifact dummyOriginatingArtifact =
                artifactFactory.createBuildArtifact("org.apache.maven.plugins", "maven-downloader-plugin", "1.0",
                        "jar");
        Artifact pluginContainerArtifact = this.artifactFactory.createArtifact(
                RHQ_PLATFORM_PLUGIN_GROUP_ID, RHQ_PLATFORM_PLUGIN_ARTIFACT_ID, rhqVersion,
                null, "jar");
        try {
            return artifactResolver.resolveTransitively(Collections
                    .singleton(pluginContainerArtifact), dummyOriginatingArtifact, localRepository,
                    remoteRepositories, artifactMetadataSource, null);
        } catch (Exception e) {
            throw new MojoExecutionException("Could not resolve the Platform Plugin artifact", e);
        }
    }

    private File findPlatformPluginFile(ArtifactResolutionResult platformPluginResolutionResult) throws
            MojoExecutionException {
        Iterator iterator = platformPluginResolutionResult.getArtifacts().iterator();
        while (iterator.hasNext()) {
            Artifact artifact = (Artifact) iterator.next();
            if (artifact.getGroupId().equals(RHQ_PLATFORM_PLUGIN_GROUP_ID) && artifact.getArtifactId().equals
                    (RHQ_PLATFORM_PLUGIN_ARTIFACT_ID) && artifact.getType().equals("jar")) {
                return artifact.getFile();
            }
        }
        throw new MojoExecutionException("Could not find Platform Plugin file");
    }

    private void copyRequiredPlugins(File pluginsDirectory, Set<File> requiredPlugins)
            throws MojoExecutionException {
        for (File requiredPlugin : requiredPlugins) {
            try {
                FileUtils.copyFileToDirectory(requiredPlugin, pluginsDirectory);
                break;
            } catch (Exception e) {
                throw new MojoExecutionException("Could not copy plugin file " + requiredPlugin
                        .getAbsolutePath(), e);
            }
        }
    }

    private File findSigarDistributionFile(ArtifactResolutionResult platformPluginResolutionResult)
            throws MojoExecutionException {
        Iterator iterator = platformPluginResolutionResult.getArtifacts().iterator();
        while (iterator.hasNext()) {
            Artifact artifact = (Artifact) iterator.next();
            if (artifact.getGroupId().equals(SIGAR_GROUP_ID) && artifact.getArtifactId().equals
                    (SIGAR_ARTIFACT_ID) && artifact.getType().equals("zip")) {
                return artifact.getFile();
            }
        }
        throw new MojoExecutionException("Could not find Sigar distribution file");
    }

    private void copySigarLibs(final File libDirectory, File sigarDistributionFile)
            throws MojoExecutionException {
        try {
            ZipUtil.iterate(sigarDistributionFile, new ZipEntryCallback() {
                @Override
                public void process(InputStream in, ZipEntry zipEntry) throws IOException {
                    String zipEntryName = zipEntry.getName();
                    if (zipEntryName.contains("sigar-bin/lib") && !zipEntryName.endsWith("/")) {
                        String compressedFileName = zipEntryName.substring(zipEntryName.lastIndexOf("/") + 1);
                        if (compressedFileName.endsWith(".so") || compressedFileName.endsWith(".dll") ||
                                compressedFileName.endsWith(".sl") || compressedFileName.endsWith(".dylib")
                                || compressedFileName.equals("sigar.jar")) {
                            File destinationFile = new File(libDirectory, compressedFileName);
                            FileUtils.copyStreamToFile(new RawInputStreamFacade(in), destinationFile);
                        }
                    }
                }
            });
        } catch (Exception e) {
            throw new MojoExecutionException("Could not unpack Sigar file " + sigarDistributionFile
                    .getAbsolutePath(), e);
        }
    }
}
