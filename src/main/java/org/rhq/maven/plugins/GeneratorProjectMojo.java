package org.rhq.maven.plugins;

import static org.codehaus.plexus.util.StringUtils.join;
import static org.rhq.maven.plugins.Utils.redirectOuput;

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

@Mojo(name = "generatorProject", requiresProject=false)
public class GeneratorProjectMojo extends AbstractMojo {
	
	private static final String PLUGIN_GENERATOR_MODULE_GROUP_ID = "org.rhq.helpers";
	private static final String PLUGIN_GENERATOR_MODULE_ARTIFACT_ID = "rhq-pluginGen";
	private static final String PLUGIN_GENERATOR_MAIN_CLASS = "org.rhq.helpers.pluginGen.PluginGen";
	//TODO
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
		System.out.println("Generate Project Mojo rhq maven plugins");
		Process process = null;
		try {
			String pluginValidatorClasspath = buildPluginValidatorClasspath();
			String javaCommand = buildJavaCommand();
			ProcessBuilder processBuilder = buildProcessBuilder(javaCommand,
					pluginValidatorClasspath);
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
	
	private String buildPluginValidatorClasspath() throws ArtifactNotFoundException,
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
			String pluginValidatorClasspath) {
		List<String> command = new LinkedList<String>();
		command.add(javaCommand);
		command.add("-classpath");
		command.add(pluginValidatorClasspath);
		command.add("-Dorg.apache.commons.logging.Log=org.apache.commons.logging.impl.SimpleLog");
		command.add(PLUGIN_GENERATOR_MAIN_CLASS);
		if (getLog().isDebugEnabled()) {
			getLog().debug(
					"Built command to execute = "
							+ join(command.iterator(), " "));
		}
		return new ProcessBuilder().directory(new File(System.getProperty("user.dir"))).command(command);
	}
	
	
	private void handleFailure(String message) throws MojoFailureException {
//		if (failOnError) {
//			throw new MojoFailureException(message);
//		}
		getLog().error(message);
	}

	private void handleException(Exception e) throws MojoExecutionException {
//		if (failOnError) {
//			throw new MojoExecutionException(e.getMessage(), e);
//		}
		getLog().error(e.getMessage(), e);
	}
}
