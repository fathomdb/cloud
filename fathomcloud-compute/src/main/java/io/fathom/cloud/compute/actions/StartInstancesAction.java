package io.fathom.cloud.compute.actions;

import io.fathom.cloud.CloudException;
import io.fathom.cloud.compute.scheduler.InstanceScheduler;
import io.fathom.cloud.compute.scheduler.SchedulerHost;
import io.fathom.cloud.compute.scheduler.SchedulerRequest;
import io.fathom.cloud.compute.services.AsyncTasks;
import io.fathom.cloud.compute.services.ComputeServices;
import io.fathom.cloud.protobuf.CloudModel.InstanceData;
import io.fathom.cloud.protobuf.CloudModel.InstanceState;
import io.fathom.cloud.protobuf.CloudModel.ReservationData;
import io.fathom.cloud.server.auth.Auth;
import io.fathom.cloud.server.model.Project;
import io.fathom.cloud.services.AuthService;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;

public class StartInstancesAction extends Action {
    private static final Logger log = LoggerFactory.getLogger(StartInstancesAction.class);

    @Inject
    InstanceScheduler scheduler;

    @Inject
    ComputeServices computeServices;

    @Inject
    AsyncTasks asyncTasks;

    @Inject
    AuthService authService;

    public int minCount;
    public int maxCount;

    // public User user;
    public Project project;
    public Auth auth;
    public ReservationData reservationTemplate;
    public InstanceData instanceTemplate;

    public String ip;

    // public FlavorData flavor;

    public static class Result {
        public ReservationData reservation;
        public ArrayList<InstanceData> instances;
    }

    public static class StartInstanceData {
        public InstanceData instanceInfo;
        public String token;
    }

    public Result go() throws CloudException {
        if (minCount > maxCount) {
            throw new IllegalArgumentException();
        }

        Result result = new Result();

        ReservationData reservationInfo;

        {
            ReservationData.Builder r = ReservationData.newBuilder(reservationTemplate);
            r.setProjectId(project.getId());

            reservationInfo = computeServices.createReservation(auth, project, r);

            result.reservation = reservationInfo;
        }

        SchedulerRequest schedulerRequest = new SchedulerRequest();
        schedulerRequest.minCount = minCount;
        schedulerRequest.maxCount = maxCount;
        List<SchedulerHost> hosts = scheduler.pickHosts(schedulerRequest);

        if (hosts.size() < minCount) {
            // TODO: What is the correct error message??
            throw new IllegalStateException();
        }

        long time = System.currentTimeMillis();

        result.instances = Lists.newArrayList();

        List<StartInstanceData> startInstances = Lists.newArrayList();
        for (int i = 0; i < hosts.size(); i++) {
            SchedulerHost host = hosts.get(i);
            InstanceData instanceInfo;
            {
                InstanceData.Builder b = InstanceData.newBuilder(instanceTemplate);
                b.setProjectId(project.getId());
                b.setHostId(host.getId());
                b.setReservationId(reservationInfo.getId());

                b.setInstanceState(InstanceState.PENDING);
                b.setLaunchIndex(i);

                b.setLaunchTime(time);

                instanceInfo = computeServices.createInstance(auth, project, b);
            }

            result.instances.add(instanceInfo);

            StartInstanceData data = new StartInstanceData();
            data.instanceInfo = instanceInfo;
            data.token = authService.createServiceToken(auth, instanceInfo.getId());

            startInstances.add(data);
        }

        asyncTasks.startInstances(auth, project, startInstances);

        return result;
    }
}
