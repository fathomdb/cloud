package io.fathom.cloud.openstack.client.identity.model;

import io.fathom.cloud.openstack.client.identity.model.V2AuthRequest.ChallengeResponse;

public class RegisterRequest {
    public String email;

    public ChallengeResponse challengeResponse;
}
