/*
 * RHQ Management Platform
 * Copyright 2013-2014, Red Hat Middleware LLC, and individual contributors
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
import java.net.URI;
import java.net.URISyntaxException;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.methods.AbortableHttpRequest;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.entity.FileEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.BasicClientConnectionManager;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.util.EntityUtils;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.json.JSONException;
import org.json.JSONObject;

import static java.lang.Boolean.TRUE;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.apache.http.HttpHeaders.ACCEPT;
import static org.apache.http.entity.ContentType.APPLICATION_JSON;
import static org.apache.http.entity.ContentType.APPLICATION_OCTET_STREAM;
import static org.apache.maven.plugins.annotations.LifecyclePhase.PACKAGE;
import static org.rhq.maven.plugins.Utils.getAgentPluginArchiveFile;

/**
 * Upload a freshly built RHQ Agent Plugin to an RHQ container.
 *
 * @author Thomas Segismont
 */
@Mojo(name = "upload", defaultPhase = PACKAGE, threadSafe = true)
public class UploadMojo extends AbstractMojo {

    private static final String REST_CONTEXT_PATH = "/rest";
    private static final String REST_CONTENT_URI = REST_CONTEXT_PATH + "/content";
    private static final String REST_CONTENT_UPLOAD_URI = REST_CONTENT_URI + "/fresh";
    private static final String REST_PLUGINS_URI = REST_CONTEXT_PATH + "/plugins";
    private static final String REST_PLUGINS_DEPLOY_URI = REST_PLUGINS_URI + "/deploy";

    /**
     * The build directory (root of build works).
     */
    @Parameter(defaultValue = "${project.build.directory}", required = true, readonly = true)
    private File buildDirectory;

    /**
     * The name of the generated RHQ agent plugin archive.
     */
    @Parameter(defaultValue = "${project.build.finalName}", required = true, readonly = true)
    private String finalName;

    /**
     * The scheme to use to communicate with the remote RHQ server.
     */
    @Parameter(defaultValue = "http")
    private String scheme;

    /**
     * Remote RHQ server host.
     */
    @Parameter(required = true)
    private String host;

    /**
     * Remote RHQ server port.
     */
    @Parameter(defaultValue = "7080")
    private int port;

    /**
     * Authentication user name. The user must have appropriate permissions (MANAGE_SETTINGS).
     */
    @Parameter(required = true)
    private String username;

    /**
     * Authentication password.
     */
    @Parameter(required = true)
    private String password;

    /**
     * Whether a plugin scan should be triggered on the server after upload.
     */
    @Parameter(defaultValue = "true")
    private boolean startScan;

    /**
     * Whether all agents should update their plugins. <strong>This will make your agents' plugin containers restart
     * .</strong>
     */
    @Parameter(defaultValue = "false")
    private boolean updatePluginsOnAllAgents;

    /**
     * Whether to wait for the plugins update requests to complete.
     */
    @Parameter(defaultValue = "false")
    private boolean waitForPluginsUpdateOnAllAgents;

    /**
     * How long should we wait for the plugins update requests to complete. In seconds.
     */
    @Parameter(defaultValue = "300")
    private long maxWaitForPluginsUpdateOnAllAgents;

    /**
     * Whether to fail the build if an error occurs while uploading.
     */
    @Parameter(defaultValue = "true")
    private boolean failOnError;

    /**
     * Whether to skip the execution of this mojo.
     */
    @Parameter(defaultValue = "false")
    private boolean skipUpload;

    /**
     * In milliseconds.
     */
    @Parameter(defaultValue = "60000")
    private int socketConnectionTimeout;

    /**
     * In milliseconds.
     */
    @Parameter(defaultValue = "300000")
    private int socketReadTimeout;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        if (skipUpload) {
            getLog().info("Skipped execution");
            return;
        }
        File agentPluginArchive = getAgentPluginArchiveFile(buildDirectory, finalName);
        if (!agentPluginArchive.exists() && agentPluginArchive.isFile()) {
            throw new MojoExecutionException("Agent plugin archive does not exist: " + agentPluginArchive);
        }

        // Prepare HttpClient
        ClientConnectionManager httpConnectionManager = new BasicClientConnectionManager();
        DefaultHttpClient httpClient = new DefaultHttpClient(httpConnectionManager);
        HttpParams httpParams = httpClient.getParams();
        HttpConnectionParams.setConnectionTimeout(httpParams, socketConnectionTimeout);
        HttpConnectionParams.setSoTimeout(httpParams, socketReadTimeout);
        httpClient.getCredentialsProvider().setCredentials(new AuthScope(host, port),
                new UsernamePasswordCredentials(username, password));

        HttpPost uploadContentRequest = null;
        HttpPut moveContentToPluginsDirRequest = null;
        HttpPost pluginScanRequest = null;
        HttpPost pluginDeployRequest = null;
        HttpGet pluginDeployCheckCompleteRequest = null;
        try {

            // Upload plugin content
            URI uploadContentUri = buildUploadContentUri();
            uploadContentRequest = new HttpPost(uploadContentUri);
            uploadContentRequest.setEntity(new FileEntity(agentPluginArchive, APPLICATION_OCTET_STREAM));
            uploadContentRequest.setHeader(ACCEPT, APPLICATION_JSON.getMimeType());
            HttpResponse uploadContentResponse = httpClient.execute(uploadContentRequest);

            if (uploadContentResponse.getStatusLine().getStatusCode() != HttpStatus.SC_CREATED) {
                handleProblem(uploadContentResponse.getStatusLine().toString());
                return;
            }

            getLog().info("Uploaded " + agentPluginArchive);
            // Read the content handle value in JSON response
            JSONObject uploadContentResponseJsonObject = new JSONObject(EntityUtils.toString(uploadContentResponse
                    .getEntity()));
            String contentHandle = (String) uploadContentResponseJsonObject.get("value");
            uploadContentRequest.abort();

            if (!startScan && !updatePluginsOnAllAgents) {

                // Request uploaded content to be moved to the plugins directory but do not trigger a plugin scan
                URI moveContentToPluginsDirUri = buildMoveContentToPluginsDirUri(contentHandle);
                moveContentToPluginsDirRequest = new HttpPut(moveContentToPluginsDirUri);
                moveContentToPluginsDirRequest.setHeader(ACCEPT, APPLICATION_JSON.getMimeType());
                HttpResponse moveContentToPluginsDirResponse = httpClient.execute(moveContentToPluginsDirRequest);

                if (moveContentToPluginsDirResponse.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
                    handleProblem(moveContentToPluginsDirResponse.getStatusLine().toString());
                    return;
                }

                moveContentToPluginsDirRequest.abort();
                getLog().info("Moved uploaded content to plugins directory");
                return;
            }

            // Request uploaded content to be moved to the plugins directory and trigger a plugin scan
            URI pluginScanUri = buildPluginScanUri(contentHandle);
            pluginScanRequest = new HttpPost(pluginScanUri);
            pluginScanRequest.setHeader(ACCEPT, APPLICATION_JSON.getMimeType());
            getLog().info("Moving uploaded content to plugins directory and requesting a plugin scan");
            HttpResponse pluginScanResponse = httpClient.execute(pluginScanRequest);

            if (pluginScanResponse.getStatusLine().getStatusCode() != HttpStatus.SC_CREATED //
                    && pluginScanResponse.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
                handleProblem(pluginScanResponse.getStatusLine().toString());
                return;
            }

            pluginScanRequest.abort();
            getLog().info("Plugin scan complete");

            if (updatePluginsOnAllAgents) {

                URI pluginDeployUri = buildPluginDeployUri();
                pluginDeployRequest = new HttpPost(pluginDeployUri);
                pluginDeployRequest.setHeader(ACCEPT, APPLICATION_JSON.getMimeType());
                getLog().info("Requesting agents to update their plugins");
                HttpResponse pluginDeployResponse = httpClient.execute(pluginDeployRequest);

                if (pluginDeployResponse.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
                    handleProblem(pluginDeployResponse.getStatusLine().toString());
                    return;
                }

                getLog().info("Plugins update requests sent");
                // Read the agent plugins update handle value in JSON response
                JSONObject pluginDeployResponseJsonObject = new JSONObject(EntityUtils.toString(pluginDeployResponse
                        .getEntity()));
                String pluginsUpdateHandle = (String) pluginDeployResponseJsonObject.get("value");
                pluginDeployRequest.abort();

                if (waitForPluginsUpdateOnAllAgents) {

                    getLog().info("Waiting for plugins update requests to complete");

                    long start = System.currentTimeMillis();
                    for (; ; ) {

                        URI pluginDeployCheckCompleteUri = buildPluginDeployCheckCompleteUri(pluginsUpdateHandle);
                        pluginDeployCheckCompleteRequest = new HttpGet(pluginDeployCheckCompleteUri);
                        pluginDeployCheckCompleteRequest.setHeader(ACCEPT, APPLICATION_JSON.getMimeType());
                        HttpResponse pluginDeployCheckCompleteResponse = httpClient.execute
                                (pluginDeployCheckCompleteRequest);

                        if (pluginDeployCheckCompleteResponse.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
                            handleProblem(pluginDeployCheckCompleteResponse.getStatusLine().toString());
                            return;
                        }

                        // Read the agent plugins update handle value in JSON response
                        JSONObject pluginDeployCheckCompleteResponseJsonObject = new JSONObject(EntityUtils.toString
                                (pluginDeployCheckCompleteResponse.getEntity()));
                        Boolean pluginDeployCheckCompleteHandle = (Boolean)
                                pluginDeployCheckCompleteResponseJsonObject.get("value");
                        pluginDeployCheckCompleteRequest.abort();

                        if (pluginDeployCheckCompleteHandle == TRUE) {
                            getLog().info("All agents updated their plugins");
                            return;
                        }

                        if (SECONDS.toMillis(maxWaitForPluginsUpdateOnAllAgents) < (System.currentTimeMillis() -
                                start)) {
                            handleProblem("Not all agents updated their plugins but wait limit has been reached (" +
                                    maxWaitForPluginsUpdateOnAllAgents + " ms)");
                            return;
                        }

                        Thread.sleep(SECONDS.toMillis(5));
                        getLog().info("Checking plugins update requests status again");
                    }
                }
            }
        } catch (IOException e) {
            handleException(e);
        } catch (JSONException e) {
            handleException(e);
        } catch (URISyntaxException e) {
            handleException(e);
        } catch (InterruptedException e) {
            handleException(e);
        } finally {
            abortQuietly(uploadContentRequest);
            abortQuietly(moveContentToPluginsDirRequest);
            abortQuietly(pluginScanRequest);
            abortQuietly(pluginDeployRequest);
            abortQuietly(pluginDeployCheckCompleteRequest);
            httpConnectionManager.shutdown();
        }
    }

    private void abortQuietly(AbortableHttpRequest httpRequest) {
        if (httpRequest != null) {
            httpRequest.abort();
        }
    }

    private void handleProblem(String message) throws MojoExecutionException {
        if (failOnError) {
            throw new MojoExecutionException(message);
        }
        getLog().error(message);
    }

    private void handleException(Exception e) throws MojoExecutionException {
        if (failOnError) {
            throw new MojoExecutionException(e.getMessage(), e);
        }
        getLog().error(e.getMessage(), e);
    }

    private URI buildUploadContentUri() throws URISyntaxException {
        return new URIBuilder() //
                .setScheme(scheme) //
                .setHost(host) //
                .setPort(port) //
                .setPath(REST_CONTENT_UPLOAD_URI) //
                .build();
    }

    private URI buildMoveContentToPluginsDirUri(String contentHandle) throws URISyntaxException {
        return new URIBuilder() //
                .setScheme(scheme) //
                .setHost(host) //
                .setPort(port) //
                .setPath(REST_CONTENT_URI + "/" + contentHandle + "/plugins") //
                .setParameter("name", getAgentPluginArchiveFile(buildDirectory, finalName).getName()) //
                .setParameter("startScan", String.valueOf(false)) //
                .build();
    }

    private URI buildPluginScanUri(String contentHandle) throws URISyntaxException {
        return new URIBuilder() //
                .setScheme(scheme) //
                .setHost(host) //
                .setPort(port) //
                .setPath(REST_PLUGINS_URI) //
                .setParameter("handle", contentHandle) //
                .setParameter("name", getAgentPluginArchiveFile(buildDirectory, finalName).getName()) //
                .build();
    }

    private URI buildPluginDeployUri() throws URISyntaxException {
        return new URIBuilder() //
                .setScheme(scheme) //
                .setHost(host) //
                .setPort(port) //
                .setPath(REST_PLUGINS_DEPLOY_URI) //
                .build();
    }

    private URI buildPluginDeployCheckCompleteUri(String pluginsUpdateHandle) throws URISyntaxException {
        return new URIBuilder() //
                .setScheme(scheme) //
                .setHost(host) //
                .setPort(port) //
                .setPath(REST_PLUGINS_DEPLOY_URI + "/" + pluginsUpdateHandle) //
                .build();
    }
}
