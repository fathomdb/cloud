package io.fathom.cloud.compute.api.os.model;

public class Hypervisor {

    public long id;

    public String hypervisor_hostname;

    public Long current_workload;
    public Long disk_available_least;
    public Long free_disk_gb;
    public Long free_ram_mb;
    public Long local_gb;
    public Long local_gb_used;
    public Long memory_mb;
    public Long memory_mb_used;
    public Long running_vms;
    public Long vcpus;
    public Long vcpus_used;

    // For statistics
    public Long count;

    // elem.set('hypervisor_type')
    // elem.set('hypervisor_version')
    // elem.set('current_workload')
    // elem.set('cpu_info')
    //
    // service = xmlutil.SubTemplateElement(elem, 'service',
    // selector='service')
    // service.set('id')
    // service.set('host')

    // "hypervisors": [
    // {
    // "cpu_info": "?",
    // "current_workload": 0,
    // "disk_available_least": null,
    // "free_disk_gb": 1028,
    // "free_ram_mb": 7680,
    // "hypervisor_hostname": "fake-mini",
    // "hypervisor_type": "fake",
    // "hypervisor_version": 1,
    // "id": 1,
    // "local_gb": 1028,
    // "local_gb_used": 0,
    // "memory_mb": 8192,
    // "memory_mb_used": 512,
    // "running_vms": 0,
    // "service": {
    // "host": "1e0d7892083548cfb347e782d3b20342",
    // "id": 2
    // },
    // "vcpus": 1,
    // "vcpus_used": 0
    // }
    // ]

}
