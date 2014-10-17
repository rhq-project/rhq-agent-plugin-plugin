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

import java.io.File;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.exporter.ZipExporter;
import org.jboss.shrinkwrap.api.spec.WebArchive;

/**
 * @author Thomas Segismont
 */
public class FakeRhqWarFactory {

    public static void main(String[] args) {
        WebArchive fakeRhq = ShrinkWrap.create(WebArchive.class, "fake-rhq.war");
        fakeRhq.addClass(FakeRhqApplication.class);
        fakeRhq.addClass(FakeContentEndpoint.class);
        fakeRhq.addClass(FakePluginsEndpoint.class);
        fakeRhq.addClass(StringValue.class);
        fakeRhq.addClass(BooleanValue.class);
        fakeRhq.addAsWebInfResource("jboss-web.xml", "jboss-web.xml");
        fakeRhq.as(ZipExporter.class).exportTo(new File("target", fakeRhq.getName()), true);
    }
}
