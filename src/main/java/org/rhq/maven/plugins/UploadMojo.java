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
import java.net.URI;
import java.net.URISyntaxException;

import org.apache.http.HttpHeaders;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.FileEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.BasicClientConnectionManager;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.util.EntityUtils;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.json.JSONException;
import org.json.JSONObject;

import static org.rhq.maven.plugins.Utils.getAgentPluginArchiveFile;

/**
 * Upload a freshly built RHQ Agent Plugin to an RHQ container.
 *
 * @author Thomas Segismont
 */
@Mojo(name = "upload", defaultPhase = LifecyclePhase.PACKAGE, threadSafe = true)
public class UploadMojo extends AbstractMojo {

    private static final String REST_CONTENT_URI = "/rest/content";
    private static final String UPLOAD_URI = REST_CONTENT_URI + "/fresh";

    private static final int SOCKET_CONNECTION_TIMEOUT = 1000 * 30;
    private static final int SOCKET_READ_TIMEOUT = 1000 * 30;

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
     * Whether to fail the build if an error occurs while uploading.
     */
    @Parameter(defaultValue = "true")
    private boolean failOnError;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        File agentPluginArchive = getAgentPluginArchiveFile(buildDirectory, finalName);
        if (!agentPluginArchive.exists() && agentPluginArchive.isFile()) {
            throw new MojoExecutionException("Agent plugin archive does not exist: " + agentPluginArchive);
        }

        // Prepare HttpClient for plugin upload
        ClientConnectionManager httpConnectionManager = new BasicClientConnectionManager();
        DefaultHttpClient httpClient = new DefaultHttpClient(httpConnectionManager);
        HttpParams httpParams = httpClient.getParams();
        HttpConnectionParams.setConnectionTimeout(httpParams, SOCKET_CONNECTION_TIMEOUT);
        HttpConnectionParams.setSoTimeout(httpParams, SOCKET_READ_TIMEOUT);
        httpClient.getCredentialsProvider().setCredentials(new AuthScope(host, port),
                new UsernamePasswordCredentials(username, password));

        HttpPost uploadContentRequest = null;
        HttpPut moveContentToPluginsDirRequest = null;
        try {
            // Upload plugin content
            URI uploadContentUri = buildUploadContentUri();
            uploadContentRequest = new HttpPost(uploadContentUri);
            uploadContentRequest.setEntity(new FileEntity(agentPluginArchive, ContentType.APPLICATION_OCTET_STREAM));
            uploadContentRequest.setHeader(HttpHeaders.ACCEPT, ContentType.APPLICATION_JSON.getMimeType());
            HttpResponse uploadContentResponse = httpClient.execute(uploadContentRequest);
            if (uploadContentResponse.getStatusLine().getStatusCode() != HttpStatus.SC_CREATED) {
                handleProblem(uploadContentResponse.getStatusLine().toString());
            }
            JSONObject uploadContentResponseJsonObject = new JSONObject(EntityUtils.toString(uploadContentResponse
                    .getEntity()));
            // Read the content handle value in JSON response 
            String contentHandle = (String) uploadContentResponseJsonObject.get("value");
            getLog().info("Uploaded " + agentPluginArchive);

            // Request uploaded to be moved to the plugins directory
            URI moveContentToPluginsDirUri = buildMoveContentToPluginsDirUri(contentHandle);
            moveContentToPluginsDirRequest = new HttpPut(moveContentToPluginsDirUri);
            moveContentToPluginsDirRequest.setHeader(HttpHeaders.ACCEPT, ContentType.APPLICATION_JSON.getMimeType());
            HttpResponse moveContentToPluginsDirResponse = httpClient.execute(moveContentToPluginsDirRequest);
            if (moveContentToPluginsDirResponse.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
                handleProblem(moveContentToPluginsDirResponse.getStatusLine().toString());
            }
            if (startScan) {
                getLog().info("Moved uploaded content to plugins directory and triggered plugin scan");
            } else {
                getLog().info("Moved uploaded content to plugins directory");
            }
        } catch (IOException e) {
            handleException(e);
        } catch (JSONException e) {
            handleException(e);
        } catch (URISyntaxException e) {
            handleException(e);
        } finally {
            if (uploadContentRequest != null) {
                uploadContentRequest.abort();
            }
            if (moveContentToPluginsDirRequest != null) {
                moveContentToPluginsDirRequest.abort();
            }
            httpConnectionManager.shutdown();
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
                .setPath(UPLOAD_URI) //
                .build();
    }

    private URI buildMoveContentToPluginsDirUri(String contentHandle) throws URISyntaxException {
        return new URIBuilder() //
                .setScheme(scheme) //
                .setHost(host) //
                .setPort(port) //
                .setPath(REST_CONTENT_URI + "/" + contentHandle + "/plugins") //
                .setParameter("name", getAgentPluginArchiveFile(buildDirectory, finalName).getName()) //
                .setParameter("startScan", String.valueOf(startScan)) //
                .build();
    }
}
