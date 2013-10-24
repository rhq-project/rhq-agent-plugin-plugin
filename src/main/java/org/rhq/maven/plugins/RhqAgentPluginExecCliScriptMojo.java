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
    private static final String INVALID_ARGS_STYLE_CONFIGURATION = "Invalid argsStyle configuration";

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
            handleProblem(scriptFile + " does not exist");
        }
        if (!VALID_ARGS_STYLES.contains(argsStyle)) {
            handleProblem(INVALID_ARGS_STYLE_CONFIGURATION + ": " + argsStyle);
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
                    .command(getRhqCliCommand(rhqCliStartScriptFile));
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

    private List<String> getRhqCliCommand(File rhqCliStartScriptFile) {
        List<String> command = new LinkedList<String>();
        command.add(rhqCliStartScriptFile.getAbsolutePath());
        if (login) {
            command.add("-u");
            command.add(username);
            command.add("-p");
            command.add(password);
            command.add("-s");
            command.add(host);
            command.add("-t");
            command.add(String.valueOf(port));
        }
        command.add("--args-style");
        command.add(argsStyle);
        command.add("-f");
        command.add(scriptFile.getAbsolutePath());
        if (getLog().isDebugEnabled()) {
            getLog().debug("RHQ CLI command = " + StringUtils.join(command.iterator(), " "));
        }
        if (args != null && !args.isEmpty()) {
            if (getLog().isDebugEnabled()) {
                getLog().debug("RHQ CLI args = " + args);
            }
            command.addAll(args);
        }
        return command;
    }
}
