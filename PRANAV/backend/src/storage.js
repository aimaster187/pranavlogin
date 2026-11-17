import fs from "node:fs";
import path from "node:path";

const DEFAULT_STATE = () => ({
  workspace: {
    masterMaintenance: false,
    maintenanceMessage: "Scheduled maintenance in progress.",
    emergencyLockdown: false
  },
  apps: [],
  dialogs: [],
  devices: [],
  licenses: [],
  bots: [],
  vault: {},
  events: [],
  updateConfirmations: [],
  nonces: {}
});

export class PranavStorage {
  constructor(dataDir) {
    this.dataDir = path.resolve(dataDir);
    this.dbFile = path.join(this.dataDir, "pranav_store.json");
    fs.mkdirSync(this.dataDir, { recursive: true });
    this.state = this._load();
  }

  _load() {
    if (!fs.existsSync(this.dbFile)) {
      const init = DEFAULT_STATE();
      this._persist(init);
      return init;
    }
    try {
      const parsed = JSON.parse(fs.readFileSync(this.dbFile, "utf8"));
      return { ...DEFAULT_STATE(), ...parsed };
    } catch {
      const fallback = DEFAULT_STATE();
      this._persist(fallback);
      return fallback;
    }
  }

  _persist(nextState = this.state) {
    const tmp = `${this.dbFile}.tmp`;
    fs.writeFileSync(tmp, JSON.stringify(nextState, null, 2), "utf8");
    fs.renameSync(tmp, this.dbFile);
  }

  save() {
    this._persist(this.state);
  }

  resolveIsolatedApp(appIdentifier) {
    return this.state.apps.find(
      (a) => a.id === appIdentifier || a.appCode === appIdentifier || a.packageName === appIdentifier
    );
  }

  getDialogsForApp(appId, appCode) {
    return this.state.dialogs
      .filter((d) => d.enabled !== false && (d.appId === appId || d.appId === appCode || d.appId === "global"))
      .sort((a, b) => Number(b.priority || 0) - Number(a.priority || 0));
  }

  appendEvent(category, title, message, severity = "INFO", appId = "global") {
    const evt = {
      id: `evt_${Date.now()}_${Math.random().toString(36).slice(2, 8)}`,
      appId,
      category,
      title,
      message,
      severity,
      status: "OPEN",
      createdAtMs: Date.now()
    };
    this.state.events.unshift(evt);
    if (this.state.events.length > 500) {
      this.state.events.length = 500;
    }
    this.save();
    return evt;
  }
}
