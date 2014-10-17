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

package org.rhg.maven.plugins.itest;

import java.net.URI;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.UriInfo;

import static java.util.concurrent.TimeUnit.SECONDS;

/**
 * @author Thomas Segismont
 */
@Path("/plugins")
public class FakePluginsEndpoint {

    private static final AtomicInteger HANDLE_GENERATOR = new AtomicInteger(0);
    private static final Map<String, Long> HANDLES_MAP = new ConcurrentHashMap<String, Long>();

    public FakePluginsEndpoint() {}

    @POST
    @Path("/")
    public Response register(@QueryParam("handle") String handle, @QueryParam("name") String name,
                             @Context HttpHeaders headers, @Context UriInfo uriInfo) throws Exception {
        URI myPluginUri = uriInfo.getBaseUri().resolve(uriInfo.getPath()).resolve(Integer.toString(1));
        ResponseBuilder builder = Response.created(myPluginUri).type(headers.getAcceptableMediaTypes().get(0));
        return builder.build();
    }

    @POST
    @Path("/deploy")
    public Response deployOnAgents(@QueryParam("delay") @DefaultValue("0") long delay,
                                   @Context HttpHeaders headers) throws Exception {
        String handle = String.valueOf(HANDLE_GENERATOR.incrementAndGet());
        HANDLES_MAP.put(handle, System.currentTimeMillis());
        return Response.ok(new StringValue(handle)).type(headers.getAcceptableMediaTypes().get(0)).build();
    }

    @GET
    @Path("/deploy/{handle}")
    public Response isUpdateFinished(@PathParam("handle") String handle, @Context HttpHeaders headers) {
        Long start = HANDLES_MAP.get(handle);
        boolean isFinished = start != null && ((System.currentTimeMillis() - start) > SECONDS.toMillis(30));
        return Response.ok(new BooleanValue(isFinished)).type(headers.getAcceptableMediaTypes().get(0)).build();
    }
}
