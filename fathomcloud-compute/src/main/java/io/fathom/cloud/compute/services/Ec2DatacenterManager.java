package io.fathom.cloud.compute.services;

import io.fathom.cloud.compute.scheduler.SchedulerHost;
import io.fathom.cloud.protobuf.CloudModel.HostData;
import io.fathom.cloud.protobuf.CloudModel.HostGroupData;
import io.fathom.cloud.protobuf.CloudModel.HostGroupSecretData;
import io.fathom.cloud.protobuf.CloudModel.HostGroupType;

import java.io.IOException;
import java.net.URI;
import java.text.ParseException;
import java.util.Date;

import com.amazonaws.AmazonClientException;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.auth.BasicSessionCredentials;
import com.amazonaws.auth.InstanceProfileCredentialsProvider;
import com.amazonaws.internal.EC2MetadataClient;
import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.util.DateUtils;
import com.amazonaws.util.json.JSONException;
import com.amazonaws.util.json.JSONObject;
import com.google.common.base.Strings;

public class Ec2DatacenterManager implements DatacenterManager {

    final HostGroupData hostGroupData;
    AmazonEC2Client ec2Client;

    public Ec2DatacenterManager(HostGroupData hostGroupData, HostGroupSecretData secretData) {
        this.hostGroupData = hostGroupData;

        if (hostGroupData.getHostGroupType() != HostGroupType.HOST_GROUP_TYPE_AMAZON_EC2) {
            throw new IllegalStateException();
        }

        if (secretData.hasUsername()) {
            String accessKey = secretData.getUsername();
            String secretKey = secretData.getPassword();
            AWSCredentials awsCredentials = new BasicAWSCredentials(accessKey, secretKey);
            ec2Client = new AmazonEC2Client(awsCredentials);
        }
    }

    public AmazonEC2Client getEc2Client(SchedulerHost host) {
        if (this.ec2Client != null) {
            return this.ec2Client;
        }

        CredentialsProvider credentialsProvider = new CredentialsProvider(host);
        AmazonEC2Client ec2Client = new AmazonEC2Client(credentialsProvider);
        return ec2Client;
    }

    /**
     * Borrowed from AWS SDK (Apache licensed)
     * InstanceProfileCredentialsProvider
     */
    class CredentialsProvider extends InstanceProfileCredentialsProvider {
        private final SchedulerHost host;

        protected volatile AWSCredentials credentials;
        protected volatile Date credentialsExpiration;

        public CredentialsProvider(SchedulerHost host) {
            this.host = host;
        }

        @Override
        public AWSCredentials getCredentials() {
            if (needsToLoadCredentials()) {
                loadCredentials();
            }
            if (expired()) {
                throw new AmazonClientException(
                        "The credentials received from the Amazon EC2 metadata service have expired");
            }

            return credentials;
        }

        private boolean expired() {
            if (credentialsExpiration != null) {
                if (credentialsExpiration.getTime() < System.currentTimeMillis()) {
                    return true;
                }
            }

            return false;
        }

        private synchronized void loadCredentials() {
            if (needsToLoadCredentials()) {
                try {
                    // String credentialsResponse = new
                    // EC2MetadataClient().getDefaultCredentials();
                    String credentialsResponse = getDefaultCredentials();

                    JSONObject jsonObject = new JSONObject(credentialsResponse);

                    if (jsonObject.has("Token")) {
                        credentials = new BasicSessionCredentials(jsonObject.getString("AccessKeyId"),
                                jsonObject.getString("SecretAccessKey"), jsonObject.getString("Token"));
                    } else {
                        credentials = new BasicAWSCredentials(jsonObject.getString("AccessKeyId"),
                                jsonObject.getString("SecretAccessKey"));
                    }

                    if (jsonObject.has("Expiration")) {
                        /*
                         * TODO: The expiration string comes in a different
                         * format than what we deal with in other parts of the
                         * SDK, so we have to convert it to the ISO8601 syntax
                         * we expect.
                         */
                        String expiration = jsonObject.getString("Expiration");
                        expiration = expiration.replaceAll("\\+0000$", "Z");

                        credentialsExpiration = new DateUtils().parseIso8601Date(expiration);
                    }
                } catch (IOException e) {
                    throw new AmazonClientException("Unable to load credentials from Amazon EC2 metadata service", e);
                } catch (JSONException e) {
                    throw new AmazonClientException("Unable to parse credentials from Amazon EC2 metadata service", e);
                } catch (ParseException e) {
                    throw new AmazonClientException(
                            "Unable to parse credentials expiration date from Amazon EC2 metadata service", e);
                }
            }

        }

        private String getDefaultCredentials() throws IOException {
            String securityCredentialsList = readResource(EC2MetadataClient.SECURITY_CREDENTIALS_RESOURCE);

            securityCredentialsList = securityCredentialsList.trim();
            String[] securityCredentials = securityCredentialsList.split("\n");
            if (securityCredentials.length == 0) {
                return null;
            }

            String securityCredentialsName = securityCredentials[0];

            return readResource(EC2MetadataClient.SECURITY_CREDENTIALS_RESOURCE + securityCredentialsName);
        }

        private String readResource(String resourcePath) throws IOException {
            String url = "http://169.254.169.254" + resourcePath;

            URI uri = URI.create(url);
            return host.fetchUrl(uri);
        }
    }

    public String findHost(SchedulerHost host) {
        HostData hostData = host.getHostData();
        if (hostData.getHostGroup() != hostGroupData.getId()) {
            throw new IllegalStateException();
        }

        String providerId = hostData.getProviderId();
        if (Strings.isNullOrEmpty(providerId)) {
            throw new IllegalStateException();
        }

        return providerId;
    }

}
