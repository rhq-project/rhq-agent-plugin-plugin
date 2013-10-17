package test.plugin;

import java.util.Set;

import org.rhq.core.domain.measurement.AvailabilityType;
import org.rhq.core.domain.measurement.MeasurementReport;
import org.rhq.core.domain.measurement.MeasurementScheduleRequest;
import org.rhq.core.pluginapi.inventory.InvalidPluginConfigurationException;
import org.rhq.core.pluginapi.inventory.ResourceComponent;
import org.rhq.core.pluginapi.inventory.ResourceContext;
import org.rhq.core.pluginapi.measurement.MeasurementFacet;

public class SimpleServiceComponent implements ResourceComponent, MeasurementFacet {

    @Override
    public void start(ResourceContext resourceContext) throws InvalidPluginConfigurationException {
    }

    @Override
    public void stop() {
    }

    @Override
    public AvailabilityType getAvailability() {
        return null;
    }

    @Override
    public void getValues(MeasurementReport report, Set<MeasurementScheduleRequest> metrics) throws Exception {
    }
}
