package test.plugin;

import java.util.Set;

import org.rhq.core.pluginapi.inventory.DiscoveredResourceDetails;
import org.rhq.core.pluginapi.inventory.InvalidPluginConfigurationException;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryComponent;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryContext;

public class SimpleServiceDiscoveryComponent implements ResourceDiscoveryComponent {

    @Override
    public Set<DiscoveredResourceDetails> discoverResources(ResourceDiscoveryContext context)
        throws InvalidPluginConfigurationException, Exception {
        return null;
    }
}
