package io.fathom.cloud.compute;

import io.fathom.cloud.compute.api.os.resources.CertificatesResource;
import io.fathom.cloud.compute.api.os.resources.ComputeImagesResource;
import io.fathom.cloud.compute.api.os.resources.ExtensionsResource;
import io.fathom.cloud.compute.api.os.resources.FlavorsResource;
import io.fathom.cloud.compute.api.os.resources.FloatingIpPoolsResource;
import io.fathom.cloud.compute.api.os.resources.FloatingIpsResource;
import io.fathom.cloud.compute.api.os.resources.HostsResource;
import io.fathom.cloud.compute.api.os.resources.KeypairsResource;
import io.fathom.cloud.compute.api.os.resources.LimitsResource;
import io.fathom.cloud.compute.api.os.resources.OsAggregatesResource;
import io.fathom.cloud.compute.api.os.resources.OsAvailabilityZoneResource;
import io.fathom.cloud.compute.api.os.resources.OsFloatingIpDnsResource;
import io.fathom.cloud.compute.api.os.resources.OsHypervisorsResource;
import io.fathom.cloud.compute.api.os.resources.OsServicesResource;
import io.fathom.cloud.compute.api.os.resources.QuotaSetsResource;
import io.fathom.cloud.compute.api.os.resources.SecurityGroupRulesResource;
import io.fathom.cloud.compute.api.os.resources.SecurityGroupsResource;
import io.fathom.cloud.compute.api.os.resources.ServerMetadataResource;
import io.fathom.cloud.compute.api.os.resources.ServersResource;
import io.fathom.cloud.compute.api.os.resources.SimpleTenantUsageResource;
import io.fathom.cloud.compute.commands.TimeSpanOptionHandler;
import io.fathom.cloud.compute.metadata.MetadataFilter;
import io.fathom.cloud.compute.metadata.MetadataResource;
import io.fathom.cloud.compute.services.ComputeDerivedMetadata;
import io.fathom.cloud.compute.services.ComputeDerivedMetadataImpl;
import io.fathom.cloud.compute.services.ComputeService;
import io.fathom.cloud.compute.services.ComputeServiceImpl;
import io.fathom.cloud.compute.services.Flavors;
import io.fathom.cloud.compute.services.NetworkMap;
import io.fathom.cloud.compute.services.NetworkMapImpl;

import org.kohsuke.args4j.CmdLineParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fathomdb.TimeSpan;
import com.fathomdb.extensions.ExtensionModuleBase;
import com.fathomdb.extensions.HttpConfiguration;

public class ComputeExtension extends ExtensionModuleBase {

    private static final Logger log = LoggerFactory.getLogger(ComputeExtension.class);

    @Override
    public void addHttpExtensions(HttpConfiguration http) {
        // boolean ENABLE_EC2 = false;
        // if (ENABLE_EC2) {
        // http.bind(Ec2Endpoint.class);
        // }
        //
        // if (ENABLE_EC2) {
        // http.filter("/*").through(AwsFilter.class);
        // }

        http.bind(MetadataFilter.class);
        http.bind(MetadataResource.class);

        http.bind(ComputeImagesResource.class);
        http.bind(ExtensionsResource.class);
        http.bind(FlavorsResource.class);
        http.bind(FloatingIpsResource.class);
        http.bind(FloatingIpPoolsResource.class);
        http.bind(KeypairsResource.class);
        http.bind(LimitsResource.class);
        http.bind(SecurityGroupsResource.class);
        http.bind(SecurityGroupRulesResource.class);
        http.bind(ServerMetadataResource.class);
        http.bind(ServersResource.class);
        http.bind(SimpleTenantUsageResource.class);
        http.bind(QuotaSetsResource.class);
        http.bind(CertificatesResource.class);
        http.bind(OsAggregatesResource.class);
        http.bind(OsAvailabilityZoneResource.class);
        http.bind(OsFloatingIpDnsResource.class);
        http.bind(OsHypervisorsResource.class);
        http.bind(OsServicesResource.class);

        http.bind(HostsResource.class);
    }

    @Override
    protected void configure() {
        CmdLineParser.registerHandler(TimeSpan.class, TimeSpanOptionHandler.class);

        bind(ComputeService.class).to(ComputeServiceImpl.class);
        bind(NetworkMap.class).to(NetworkMapImpl.class);

        bind(ComputeDerivedMetadata.class).to(ComputeDerivedMetadataImpl.class);

        bind(Flavors.class).asEagerSingleton();
    }
}
