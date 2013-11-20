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
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.codehaus.plexus.util.StringUtils;

/**
 * Execute a CLI script.
 *
 * @author Thomas Segismont
 */
@Mojo(name = "exec-cli-script", defaultPhase = LifecyclePhase.PACKAGE, threadSafe = true)
public class RhqAgentPluginExecCliScriptMojo extends AbstractExecCliMojo {

    private static final List<String> VALID_ARGS_STYLES = Arrays.asList("indexed", "named");

    /**
     * The script file to execute.
     */
    @Parameter(required = true)
    private File scriptFile;

    /**
     * List of CLI arguments.
     */
    @Parameter(required = false)
    private List<String> args;

    /**
     * Indicates the style or format of arguments passed to the script.
     */
    @Parameter(defaultValue = "indexed")
    private String argsStyle;

    @Override
    protected void validateParams() throws MojoExecutionException, MojoFailureException {
        if (!scriptFile.isFile()) {
            throw new MojoExecutionException(scriptFile + " does not exist");
        }
        if (!VALID_ARGS_STYLES.contains(argsStyle)) {
            throw new MojoExecutionException("Invalid argsStyle configuration: " + argsStyle);
        }
    }

    @Override
    protected void doExecute() throws MojoExecutionException, MojoFailureException {
        Process process = null;
        try {
            File rhqCliStartScriptFile = getRhqCliStartScriptFile();
            // Run the CLI in forked process
            ProcessBuilder processBuilder = new ProcessBuilder() //
                    .directory(rhqCliStartScriptFile.getParentFile()) // bin directory
                    .command(buildRhqCliCommand(rhqCliStartScriptFile));
            getLog().info("Executing RHQ CLI script file: " + IOUtils.LINE_SEPARATOR + scriptFile.getAbsolutePath());
            process = processBuilder.start();
            redirectOuput(process);
            int exitCode = process.waitFor();
            if (exitCode != 0) {
                handleFailure("CLI stopped with status code: " + exitCode);
            }
        } catch (Exception e) {
            handleException(e);
        } finally {
            if (process != null) {
                process.destroy();
            }
        }
    }

    private List<String> buildRhqCliCommand(File rhqCliStartScriptFile) {
        List<String> commandParts = new LinkedList<String>();
        commandParts.add(rhqCliStartScriptFile.getAbsolutePath());
        if (login) {
            commandParts.add("-u");
            commandParts.add(username);
            commandParts.add("-p");
            commandParts.add(password);
            commandParts.add("-s");
            commandParts.add(host);
            commandParts.add("-t");
            commandParts.add(String.valueOf(port));
        }
        commandParts.add("--args-style");
        commandParts.add(argsStyle);
        commandParts.add("-f");
        commandParts.add(scriptFile.getAbsolutePath());
        if (args != null && !args.isEmpty()) {
            commandParts.addAll(args);
        }
        if (getLog().isDebugEnabled()) {
            getLog().debug("Built command to execute = " + StringUtils.join(commandParts.iterator(), " "));
        }
        return commandParts;
    }
}
