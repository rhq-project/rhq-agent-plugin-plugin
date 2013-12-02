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

package test;

import java.io.File;

import org.testng.annotations.AfterSuite;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;

import org.rhq.core.pc.PluginContainer;
import org.rhq.core.pc.PluginContainerConfiguration;
import org.rhq.core.pc.plugin.FileSystemPluginFinder;
import org.rhq.core.pc.plugin.PluginEnvironment;
import org.rhq.core.system.SystemInfo;
import org.rhq.core.system.SystemInfoFactory;

import static org.testng.Assert.assertTrue;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertEquals;

public class PCTest {

    public static final String PLUGIN_NAME = "TestPlugin";

    private static PluginContainer pluginContainer;

    private static PluginEnvironment pluginEnvironment;

    @BeforeSuite
    public void start() {
        File pluginDir = new File("target/itest/plugins");
        PluginContainerConfiguration pcConfig = new PluginContainerConfiguration();
        pcConfig.setPluginFinder(new FileSystemPluginFinder(pluginDir));
        pcConfig.setPluginDirectory(pluginDir);
        pcConfig.setInsideAgent(false);
        pluginContainer = PluginContainer.getInstance();
        pluginContainer.setConfiguration(pcConfig);
        pluginContainer.initialize();
        pluginEnvironment = pluginContainer.getPluginManager().getPlugin(PLUGIN_NAME);
    }

    @AfterSuite
    public void stop() {
        pluginContainer.shutdown();
    }

    @Test
    public void testNativeSystemAvailable() {
        SystemInfo systemInfo = SystemInfoFactory.createSystemInfo();
        assertTrue(systemInfo.isNative(), "Native system should be available");
    }

    @Test
    public void testPluginLoaded() {
        assertNotNull(pluginEnvironment, "Plugin not loaded");
        assertEquals(pluginEnvironment.getPluginName(), PLUGIN_NAME);
    }

}
