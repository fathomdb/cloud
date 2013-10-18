package io.fathom.cloud.compute.scheduler;

import java.util.List;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;

public class LxcConfigBuilder {
    public String hostname;

    public String ipv4;
    public String ipv4Gateway;

    public String ipv6;
    public String ipv6Gateway;

    public String rootfs;
    public String configDir;

    public String bridge;

    public Integer memoryLimitMB;

    public Integer swapMemoryLimitMB;

    public Integer cpuShares;

    public List<Volume> volumes = Lists.newArrayList();

    public static class Volume {
        public String hostPath;
        public String instancePath;
    }

    private StringBuilder sb;

    public String hwaddr;

    private void a(String s) {
        sb.append(s + "\n");
    }

    private void a(String k, String v) {
        if (v == null) {
            return;
        }
        a(k + " = " + v);
    }

    private void a() {
        a("");
    }

    public synchronized String build() {
        // We try to mirror what docker does, to maximize compatibility
        sb = new StringBuilder();

        a("# hostname");
        a("lxc.utsname = " + hostname);
        a();

        // #lxc.aa_profile = unconfined

        a("# network configuration");
        a("lxc.network.type = veth");
        a("lxc.network.flags = up");
        a("lxc.network.link", bridge);
        a("lxc.network.name = eth0");
        a("lxc.network.mtu = 1500");
        a("lxc.network.ipv4", ipv4);
        a("lxc.network.ipv4.gateway", ipv4Gateway);

        a("lxc.network.ipv6", ipv6);
        a("lxc.network.ipv6.gateway", ipv6Gateway);

        if (!Strings.isNullOrEmpty(hwaddr)) {
            a("lxc.network.hwaddr", hwaddr);
        }

        a();

        a("# root filesystem");
        a("lxc.rootfs", rootfs);
        a();

        a("# use a dedicated pts for the container (and limit the number of pseudo terminal available)");
        a("lxc.pts", "1024");
        a();

        a("# disable the main console");
        a("lxc.console = none");
        a();

        a("# no controlling tty at all");
        a("lxc.tty = 1");
        a();

        a("# no implicit access to devices");
        a("lxc.cgroup.devices.deny = a");
        a();

        a("# /dev/null and zero");
        a("lxc.cgroup.devices.allow = c 1:3 rwm");
        a("lxc.cgroup.devices.allow = c 1:5 rwm");
        a();

        a("# consoles");
        a("lxc.cgroup.devices.allow = c 5:1 rwm");
        a("lxc.cgroup.devices.allow = c 5:0 rwm");
        a("lxc.cgroup.devices.allow = c 4:0 rwm");
        a("lxc.cgroup.devices.allow = c 4:1 rwm");
        a();

        a("# /dev/urandom,/dev/random");
        a("lxc.cgroup.devices.allow = c 1:9 rwm");
        a("lxc.cgroup.devices.allow = c 1:8 rwm");
        a();

        a("# /dev/pts/* - pts namespaces are 'coming soon'");
        a("lxc.cgroup.devices.allow = c 136:* rwm");
        a("lxc.cgroup.devices.allow = c 5:2 rwm");
        a();

        a("# tuntap");
        a("lxc.cgroup.devices.allow = c 10:200 rwm");
        a();

        // # fuse
        // #lxc.cgroup.devices.allow = c 10:229 rwm

        // # rtc
        // #lxc.cgroup.devices.allow = c 254:0 rwm

        a("# standard mount point");
        a("# WARNING: procfs is a known attack vector and should probably be disabled");
        a("# if your userspace allows it. eg. see http://blog.zx2c4.com/749");
        a("lxc.mount.entry = proc " + rootfs + "/proc proc nosuid,nodev,noexec 0 0");
        a("# WARNING: sysfs is a known attack vector and should probably be disabled");
        a("# if your userspace allows it. eg. see http://bit.ly/T9CkqJ");
        a("lxc.mount.entry = sysfs " + rootfs + "/sys sysfs nosuid,nodev,noexec 0 0");
        a("lxc.mount.entry = devpts " + rootfs + "/dev/pts devpts newinstance,ptmxmode=0666,nosuid,noexec 0 0");

        // #lxc.mount.entry = varrun {{$ROOTFS}}/var/run tmpfs
        // mode=755,size=4096k,nosuid,nodev,noexec 0 0
        // #lxc.mount.entry = varlock {{$ROOTFS}}/var/lock tmpfs
        // size=1024k,nosuid,nodev,noexec 0 0
        // #lxc.mount.entry = shm {{$ROOTFS}}/dev/shm tmpfs
        // size=65536k,nosuid,nodev,noexec 0 0

        a("lxc.console = " + join(configDir, "console.log"));

        for (Volume volume : volumes) {
            String entry = volume.hostPath + " " + join(rootfs, volume.instancePath) + " none bind,rw 0 0";
            a("lxc.mount.entry", entry);
        }

        a();

        a("# drop linux capabilities (apply mainly to the user root in the container)");
        a("#  (Note: 'lxc.cap.keep' is coming soon and should replace this under the");
        a("#         security principle 'deny all unless explicitly permitted', see");
        a("#         http://sourceforge.net/mailarchive/message.php?msg_id=31054627 )");
        a("lxc.cap.drop = audit_control audit_write mac_admin mac_override mknod setfcap setpcap sys_admin sys_boot sys_module sys_nice sys_pacct sys_rawio sys_resource sys_time sys_tty_config");
        a();

        a("# limits");
        if (memoryLimitMB != null) {
            a("lxc.cgroup.memory.limit_in_bytes", memoryLimitMB + "M");
            a("lxc.cgroup.memory.soft_limit_in_bytes", memoryLimitMB + "M");
        }
        if (swapMemoryLimitMB != null) {
            a("lxc.cgroup.memory.memsw.limit_in_bytes", swapMemoryLimitMB + "M");
        }
        if (cpuShares != null) {
            a("lxc.cgroup.cpu.shares", cpuShares + "");
        }

        a();

        return sb.toString();
    }

    private static String join(String base, String extension) {
        StringBuilder s = new StringBuilder();

        s.append(base);

        if (!base.endsWith("/")) {
            s.append("/");
        }

        if (extension.startsWith("/")) {
            s.append(extension.substring(1));
        } else {
            s.append(extension);
        }

        return s.toString();
    }
}
