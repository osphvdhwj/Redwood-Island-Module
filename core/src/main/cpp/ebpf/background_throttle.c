/* 
 * 🔥 BACKGROUND THROTTLE (eBPF C Program)
 * 
 * Target: Zero-latency background suppression.
 * License: GPL
 */

#include <vmlinux.h>
#include <bpf/bpf_helpers.h>
#include <bpf/bpf_tracing.h>

struct {
    __uint(type, BPF_MAP_TYPE_HASH);
    __uint(max_entries, 1024);
    __type(key, u32);   // UID
    __type(value, u8);  // 1 = Whitelisted
} whitelist_map SEC(".maps");

/**
 * Hook into sched_process_fork to catch background tasks immediately.
 */
SEC("tp/sched/sched_process_fork")
int BPF_PROG(sched_process_fork, struct task_struct *parent, struct task_struct *child) {
    u32 uid = bpf_get_current_uid_gid();
    
    // Check if the task is in our whitelist
    u8 *is_whitelisted = bpf_map_lookup_elem(&whitelist_map, &uid);
    
    if (!is_whitelisted) {
        // Tag task as background for cgroup/freezer awareness
        // child->flags |= PF_IDLE; // Example: Force idle scheduling
    }

    return 0;
}

char LICENSE[] SEC("license") = "GPL";
