package io.fathom.cloud.dns.backend.aws;

import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.route53.AmazonRoute53;
import com.amazonaws.services.route53.AmazonRoute53Client;
import com.amazonaws.services.route53.model.Change;
import com.amazonaws.services.route53.model.ChangeAction;
import com.amazonaws.services.route53.model.ChangeBatch;
import com.amazonaws.services.route53.model.ChangeResourceRecordSetsRequest;
import com.amazonaws.services.route53.model.ChangeResourceRecordSetsResult;
import com.amazonaws.services.route53.model.CreateHostedZoneRequest;
import com.amazonaws.services.route53.model.CreateHostedZoneResult;
import com.amazonaws.services.route53.model.HostedZone;
import com.amazonaws.services.route53.model.HostedZoneAlreadyExistsException;
import com.amazonaws.services.route53.model.ListHostedZonesRequest;
import com.amazonaws.services.route53.model.ListHostedZonesResult;
import com.amazonaws.services.route53.model.ListResourceRecordSetsRequest;
import com.amazonaws.services.route53.model.ListResourceRecordSetsResult;
import com.amazonaws.services.route53.model.ResourceRecordSet;
import com.fathomdb.Configuration;
import com.google.common.base.CharMatcher;
import com.google.common.base.Objects;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

@Singleton
// If injected, a singleton
public class AwsRoute53Client {

    private static final Logger log = LoggerFactory.getLogger(AwsRoute53Client.class);

    final AmazonRoute53 restClient;

    Map<String, HostedZone> hostedZones;

    @Inject
    public AwsRoute53Client(Configuration config) {
        this(config.get("aws.route53.access"), config.get("aws.route53.secret"));
    }

    public AwsRoute53Client(String accessKey, String secretKey) {
        BasicAWSCredentials awsCredentials = new BasicAWSCredentials(accessKey, secretKey);
        this.restClient = new AmazonRoute53Client(awsCredentials);
    }

    public List<ResourceRecordSet> getResourceRecords(String awsZoneId) {
        List<ResourceRecordSet> resourceRecords = Lists.newArrayList();

        ListResourceRecordSetsResult previous = null;
        while (true) {
            ListResourceRecordSetsRequest request = new ListResourceRecordSetsRequest(awsZoneId);
            if (previous != null) {
                request.setStartRecordIdentifier(previous.getNextRecordIdentifier());
                request.setStartRecordName(previous.getNextRecordName());
                request.setStartRecordType(previous.getNextRecordType());
            }

            ListResourceRecordSetsResult response = restClient.listResourceRecordSets(request);
            for (ResourceRecordSet resourceRecordSet : response.getResourceRecordSets()) {
                resourceRecords.add(resourceRecordSet);
            }
            if (!Objects.equal(response.isTruncated(), Boolean.TRUE)) {
                break;
            }
            previous = response;
        }
        return resourceRecords;
    }

    public synchronized Map<String, HostedZone> getHostedZones() {
        if (hostedZones == null) {
            refreshHostedZones();
        }
        return hostedZones;
    }

    private synchronized void refreshHostedZones() {
        hostedZones = getHostedZones0();
    }

    private synchronized Map<String, HostedZone> getHostedZones0() {
        Map<String, HostedZone> hostedZones = Maps.newHashMap();
        String marker = null;
        while (true) {
            ListHostedZonesRequest request = new ListHostedZonesRequest();
            if (marker != null) {
                request.withMarker(marker);
            }
            ListHostedZonesResult response = restClient.listHostedZones(request);
            for (HostedZone hostedZone : response.getHostedZones()) {
                String key = CharMatcher.is('.').trimTrailingFrom(hostedZone.getName());
                hostedZones.put(key, hostedZone);
            }
            if (!Objects.equal(response.isTruncated(), Boolean.TRUE)) {
                break;
            }
            marker = response.getMarker();
        }
        return hostedZones;
    }

    public HostedZone createHostedZone(String zoneName) {
        HostedZone zone = getHostedZones().get(zoneName);
        if (zone != null) {
            return zone;
        }

        log.info("Creating Route 53 zone: {}", zoneName);

        CreateHostedZoneRequest request = new CreateHostedZoneRequest();
        request.setName(zoneName);
        request.setCallerReference("CreateHostedZoneRequest:" + zoneName);

        // HostedZoneConfig hostedZoneConfig = new HostedZoneConfig();
        // hostedZoneConfig.setComment(comment);
        // request.setHostedZoneConfig(hostedZoneConfig);

        try {
            CreateHostedZoneResult result = restClient.createHostedZone(request);
            zone = result.getHostedZone();
        } catch (HostedZoneAlreadyExistsException e) {
            refreshHostedZones();
            zone = hostedZones.get(zoneName);
        }
        if (zone == null) {
            throw new IllegalStateException();
        }
        return zone;
    }

    public void changeRecords(String hostedZoneId, List<ResourceRecordSet> create, List<ResourceRecordSet> remove) {
        List<Change> changes = Lists.newArrayList();

        for (ResourceRecordSet r : remove) {
            Change change = new Change(ChangeAction.DELETE, r);
            changes.add(change);
        }

        for (ResourceRecordSet r : create) {
            Change change = new Change(ChangeAction.CREATE, r);
            changes.add(change);
        }

        if (changes.isEmpty()) {
            log.info("No DNS record changes");
            return;
        }

        ChangeBatch changeBatch = new ChangeBatch();
        changeBatch.setChanges(changes);
        ChangeResourceRecordSetsRequest request = new ChangeResourceRecordSetsRequest();
        request.setHostedZoneId(hostedZoneId);
        request.setChangeBatch(changeBatch);

        log.info("Changing DNS records: {}", request);

        ChangeResourceRecordSetsResult response = restClient.changeResourceRecordSets(request);

        log.info("Changed records: {}", response);
    }

    public String getAwsZoneName(String domainName) {
        domainName = CharMatcher.is('.').trimTrailingFrom(domainName);

        int lastDot = domainName.lastIndexOf('.');
        if (lastDot == -1) {
            return domainName;
        }
        String suffix = domainName.substring(lastDot + 1);
        // TODO: Check for compound suffixes?

        String prefix = domainName.substring(0, lastDot);
        int lastDotPrefix = prefix.lastIndexOf('.');
        if (lastDotPrefix != -1) {
            prefix = prefix.substring(lastDotPrefix + 1);
        }

        return prefix + "." + suffix;
    }

}
