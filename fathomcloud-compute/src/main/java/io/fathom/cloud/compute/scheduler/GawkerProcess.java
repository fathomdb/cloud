package io.fathom.cloud.compute.scheduler;

import java.util.List;

public class GawkerProcess {
    public String Name;
    public List<String> Args;
    public List<String> Env;
    public String Dir;

    public String User;

    // Tags

    public String MatchExecutableName;

    public String RestartAction;
}
