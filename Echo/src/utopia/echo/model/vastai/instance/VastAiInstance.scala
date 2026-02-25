package utopia.echo.model.vastai.instance

/**
 * Represents a rented machine / accepted contract
 * @author Mikko Hilpinen
 * @since 24.02.2026, v1.4.1
 */
case class VastAiInstance(id: Int, status: InstanceStatus, label: String = "",
                          template: Option[TemplateIdentifier] = None)
{
	/*
	 *{
  "instances": {
    "id": 883,
    "label": null,
    "template_id": null,
    "template_hash_id": null,
    "template_name": null,
    "extra_env": [],
    "onstart": null,
    "jupyter_token": "53fc448d6644aa7535c6fa5498cdbedc782753e88d81b44090e54dcf1332ed30",
    "local_ipaddrs": "10.2.202.31 172.17.0.1 \n",
    "ssh_host": "ssh2281.vast.ai",
    "ssh_idx": "2281",
    "ssh_port": 10882,
    "machine_dir_ssh_port": 5300,
    "machine_id": 178,
    "bundle_id": 617,
    "start_date": 1761008618.225083,
    "end_date": 2034617753,
    "uptime_mins": null,
    "duration": 273608757.18784523,
    "cpu_arch": "amd64",
    "cpu_cores": 32,
    "cpu_cores_effective": 4,
    "cpu_name": "Xeon® Silver 4110",
    "cpu_ram": 128576,
    "cpu_util": 0,
    "mem_limit": null,
    "mem_usage": null,
    "vmem_usage": null,
    "gpu_name": "RTX A5000",
    "gpu_arch": "nvidia",
    "gpu_totalram": 24564,
    "gpu_ram": 24564,
    "gpu_util": null,
    "gpu_temp": null,
    "gpu_frac": 0.125,
    "gpu_lanes": 16,
    "gpu_mem_bw": 628.8,
    "bw_nvlink": 0,
    "disk_name": "Samsung SSD 860",
    "disk_space": 10,
    "disk_bw": 500.55,
    "disk_util": -1,
    "disk_usage": -1,
    "direct_port_count": 12,
    "direct_port_start": -1,
    "direct_port_end": -1,
    "ports": [
      8080,
      8081
    ],
    "static_ip": true,
    "public_ipaddr": "63.135.50.11",
    "geolocation": "Washington, US",
    "verification": "verified",
    "rentable": true,
    "host_id": 2,
    "hosting_type": 1,
    "min_bid": 0.02,
    "is_bid": false,
    "dph_base": 1,
    "dph_total": 1.0020740740740741,
    "dlperf": 22.229415,
    "dlperf_per_dphtotal": 22.183404974866942,
    "flops_per_dphtotal": 27.46814902424601,
    "total_flops": 27.52512,
    "score": 22.043656178546772,
    "reliability2": 0.9993661,
    "os_version": 18.04,
    "mobo_name": "S7109GM2NR",
    "pci_gen": 3,
    "pcie_bw": 11.7,
    "num_gpus": 1,
    "logo": "/static/logos/vastai_small2.png",
    "webpage": null,
    "search": {
      "gpuCostPerHour": 1,
      "diskHour": 0.002074074074074074,
      "totalHour": 1.0020740740740741,
      "discountTotalHour": 0,
      "discountedTotalPerHour": 1.0020740740740741
    },
    "instance": {
      "gpuCostPerHour": 0,
      "diskHour": 0.002074074074074074,
      "totalHour": 0.002074074074074074,
      "discountTotalHour": 0,
      "discountedTotalPerHour": 0.002074074074074074
    },
    "storage_cost": 0.14933333333333335,
    "storage_total_cost": 0.002074074074074074,
    "vram_costperhour": 0.00004079441760601181,
    "credit_balance": null,
    "credit_discount": null,
    "credit_discount_max": 0.4,
    "client_run_time": 1.1,
    "host_run_time": 2592000,
    "external": false,
    "time_remaining": "",
    "time_remaining_isbid": ""
  }
}
	 */
}
