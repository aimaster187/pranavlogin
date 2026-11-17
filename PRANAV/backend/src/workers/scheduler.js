import os from "node:os";

export function startBackgroundScheduler({ config, storage }) {
  const runSweep = () => {
    const now = Date.now();
    let dirty = false;

    for (const lic of storage.state.licenses) {
      if (lic.status === "ACTIVE" && lic.expiresAtEpochMs && Number(lic.expiresAtEpochMs) > 0) {
        const graceMs = Number(lic.graceDays || 0) * 86_400_000;
        if (now > Number(lic.expiresAtEpochMs) + graceMs) {
          lic.status = "EXPIRED";
          dirty = true;
        }
      }
    }

    // Expire replay-protection challenges older than 5 minutes
    for (const [nonce, meta] of Object.entries(storage.state.nonces || {})) {
      if (now - Number(meta.createdAtMs || 0) > 300_000) {
        delete storage.state.nonces[nonce];
        dirty = true;
      }
    }

    if (dirty) storage.save();
  };

  const timer = setInterval(runSweep, config.workerIntervalMs);
  timer.unref();
  return {
    runSweep,
    getSystemMetrics: () => {
      const totalMb = Math.round(os.totalmem() / 1048576);
      const freeMb = Math.round(os.freemem() / 1048576);
      return {
        memoryUsedMb: Math.max(0, totalMb - freeMb),
        memoryTotalMb: totalMb,
        loadAvg1m: os.loadavg()[0].toFixed(2),
        cpuCount: os.cpus().length
      };
    },
    stop: () => clearInterval(timer)
  };
}
