# 🧠 KERNEL MASTERY: eBPF Tracing & Interception

**Version:** 1.0.0
**Architecture:** CO-RE (Compile Once - Run Everywhere)
**Mandate:** Zero-latency background suppression & proactive resource fencing.

## 1. Overview
Redwood Phase 5 transitions from reactive userspace "app killing" to proactive kernel-level "interception." By loading eBPF (extended Berkeley Packet Filter) programs into the Linux kernel, we can filter, freeze, and deprioritize background activity with microsecond precision and zero context-switch overhead.

## 2. eBPF Hook Points

### A. Process Discovery (`tp/sched/sched_process_fork`)
*   **Action:** Triggers whenever a process is cloned or forked.
*   **Logic:** Immediately check the PID's UID against the `REDWOOD_WHITELIST_MAP`. If not found, tag the task with a "Background" flag in the kernel task structure.

### B. Proactive Freezing (`cgroup/freezer`)
*   **Action:** Hooks into cgroup state changes.
*   **Logic:** Automatically transition tagged background processes to the `FROZEN` state the millisecond they lose visibility, bypassing the `ActivityManager` delay.

### C. I/O Fencing (`kprobe/vfs_write`)
*   **Action:** Filters filesystem write operations.
*   **Logic:** Throttle or delay background I/O syscalls if the kernel detects "Thermal Pressure" or "CPU Contention" signals, ensuring the foreground UI thread (120Hz) always has priority.

## 3. Kernel-to-Neural Map Structure
We use eBPF **Hash Maps** and **Perf Buffers** for bi-directional communication.

*   `map_whitelist`: (Hash Map) Key: UID, Value: Flags. Managed by `KernelTracingEngine.kt`.
*   `map_metrics`: (Per-CPU Array) Key: Metric ID, Value: Raw Count. Polled by `KernelEventRelay.kt`.
*   `perf_events`: (Perf Buffer) Streams high-priority kernel alerts (e.g., unexpected OOM kills) to the Neural Core.

## 4. Failsafe: Kernel Panic Mode
The `KernelTracingEngine` implements a `GlobalDetach` signal. 
If the daemon detects a system-level watchdog timeout (3 failed UI frames):
1.  All eBPF programs are detached via `bpf_link_detach`.
2.  All maps are cleared.
3.  Standard AOSP scheduler profiles are restored.
