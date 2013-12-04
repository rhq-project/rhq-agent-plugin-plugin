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
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.codehaus.plexus.util.StringUtils;

import static org.codehaus.plexus.util.StringUtils.join;
import static org.rhq.maven.plugins.Utils.redirectOuput;

/**
 * Execute a CLI command.
 *
 * @author Thomas Segismont
 */
@Mojo(name = "exec-cli-command", defaultPhase = LifecyclePhase.PACKAGE, threadSafe = true)
public class ExecCliCommandMojo extends AbstractExecCliMojo {

    /**
     * The command to execute.
     */
    @Parameter(required = true)
    private String command;

    @Override
    protected void validateParams() throws MojoExecutionException, MojoFailureException {
        if (StringUtils.isBlank(command)) {
            throw new MojoExecutionException("'command' param is blank");
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
            getLog().info("Executing RHQ CLI command: " + IOUtils.LINE_SEPARATOR + command);
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
        commandParts.add("-c");
        commandParts.add(command);
        if (getLog().isDebugEnabled()) {
            getLog().debug("Built command to execute = " + join(commandParts.iterator(), " "));
        }
        return commandParts;
    }
}
