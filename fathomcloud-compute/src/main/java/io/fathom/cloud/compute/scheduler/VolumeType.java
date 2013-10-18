package io.fathom.cloud.compute.scheduler;

public enum VolumeType {
    /*
     * These volumes may disappear when an instance is restarted or upgraded.
     * 
     * On AWS, they are like instance disks.
     */
    Ephemeral,

    /*
     * These volumes are guaranteed; a confirmed write will persist.
     * 
     * On AWS, they are like EBS volumes.
     */
    Persistent,

    /**
     * These volumes are somewhere in between Ephemeral and Persistent.
     * 
     * The window of loss won't be more than a few minutes. We can implement
     * this using periodic backups, or async replication.
     */
    SemiPersistent
}
