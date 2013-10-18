package io.fathom.cloud.compute.api.aws.ec2.actions;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface AwsAction {

    String value();

}
