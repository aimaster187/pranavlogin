import { readJsonBody, requireOwnerAuth, sendJson } from "../httpHelpers.js";

export async function handleAdminRoutes(req, res, pathname, { config, storage }) {
  if (!pathname.startsWith("/api/v1/admin")) return false;
  requireOwnerAuth(req, config);

  if (req.method === "GET" && pathname === "/api/v1/admin/overview") {
    return sendJson(res, 200, {
      ok: true,
      workspace: storage.state.workspace,
      counts: {
        apps: storage.state.apps.length,
        dialogs: storage.state.dialogs.length,
        devices: storage.state.devices.length,
        licenses: storage.state.licenses.length,
        bots: storage.state.bots.length,
        events: storage.state.events.length
      }
    });
  }

  if (req.method === "POST" && pathname === "/api/v1/admin/apps") {
    const body = await readJsonBody(req);
    const id = body.id || `app_${Date.now()}`;
    const forceUpdate = Boolean(body.forceUpdateEnabled);
    const app = {
      id,
      appCode: String(body.appCode || id),
      packageName: String(body.packageName || "com.pranav.client"),
      displayName: String(body.displayName || "Managed App"),
      lifecycleStatus: String(body.lifecycleStatus || "ACTIVE"),
      masterEnabled: body.masterEnabled !== false,
      popupEnabled: Boolean(body.popupEnabled),
      forceUpdateEnabled: forceUpdate,
      laterEnabled: forceUpdate ? false : body.laterEnabled !== false,
      latestVersion: String(body.latestVersion || "1.0.0"),
      minSupportedVersion: String(body.minSupportedVersion || "1.0.0"),
      apkDownloadUrl: String(body.apkDownloadUrl || ""),
      updateTitle: String(body.updateTitle || "Update Available"),
      updateDescription: String(body.updateDescription || ""),
      releaseNotes: String(body.releaseNotes || ""),
      channelLink: String(body.channelLink || ""),
      button1Text: String(body.button1Text || "Update Now"),
      button2Text: String(body.button2Text || "Later"),
      rolloutPercent: Math.max(0, Math.min(100, Number(body.rolloutPercent ?? 100))),
      updatedAtMs: Date.now()
    };
    const idx = storage.state.apps.findIndex((a) => a.id === id || a.appCode === app.appCode);
    if (idx >= 0) storage.state.apps[idx] = app;
    else storage.state.apps.push(app);
    storage.save();
    return sendJson(res, 200, { ok: true, app });
  }

  const deleteAppMatch = pathname.match(/^\/api\/v1\/admin\/apps\/([^/]+)$/);
  if (req.method === "DELETE" && deleteAppMatch) {
    const targetId = deleteAppMatch[1];
    storage.state.apps = storage.state.apps.filter((a) => a.id !== targetId && a.appCode !== targetId);
    storage.save();
    return sendJson(res, 200, { ok: true, status: "DELETED" });
  }

  if (req.method === "POST" && pathname === "/api/v1/admin/dialogs") {
    const body = await readJsonBody(req);
    const id = body.id || `dlg_${Date.now()}`;
    const dlg = {
      id,
      appId: String(body.appId || "global"),
      dialogType: String(body.dialogType || "NORMAL_UPDATE"),
      title: String(body.title || "Notice"),
      description: String(body.description || ""),
      primaryButtonText: String(body.primaryButtonText || "Continue"),
      primaryActionUrl: String(body.primaryActionUrl || ""),
      secondaryButtonText: String(body.secondaryButtonText || "Later"),
      animationStyle: String(body.animationStyle || "GLASS_SLIDE"),
      soundName: String(body.soundName || "DEFAULT_CHIME"),
      dismissible: body.dismissible !== false,
      laterAllowed: body.laterAllowed !== false,
      enabled: body.enabled !== false,
      priority: Number(body.priority || 10),
      updatedAtMs: Date.now()
    };
    const idx = storage.state.dialogs.findIndex((d) => d.id === id);
    if (idx >= 0) storage.state.dialogs[idx] = dlg;
    else storage.state.dialogs.push(dlg);
    storage.save();
    return sendJson(res, 200, { ok: true, dialog: dlg });
  }

  if (req.method === "POST" && pathname === "/api/v1/admin/devices") {
    const body = await readJsonBody(req);
    const id = body.id || `dev_${Date.now()}`;
    const installUuid = String(body.installUuid || "");
    const existing = storage.state.devices.find(
      (d) => d.id === id || (installUuid && d.installUuid === installUuid && d.appId === body.appId)
    );
    const dev = {
      ...(existing || {}),
      id: existing?.id || id,
      appId: String(body.appId || existing?.appId || "global"),
      deviceLabel: String(body.deviceLabel || existing?.deviceLabel || "Android Device"),
      installUuid: installUuid || existing?.installUuid || id,
      keystoreKeyHash: String(body.keystoreKeyHash || existing?.keystoreKeyHash || ""),
      approvalStatus: String(body.approvalStatus || existing?.approvalStatus || "PENDING"),
      androidApi: Number(body.androidApi || existing?.androidApi || 35),
      riskScore: Number(body.riskScore ?? existing?.riskScore ?? 0),
      transferCount: Number(body.transferCount ?? existing?.transferCount ?? 0),
      updatedAtMs: Date.now()
    };
    const idx = storage.state.devices.findIndex((d) => d.id === dev.id);
    if (idx >= 0) storage.state.devices[idx] = dev;
    else storage.state.devices.push(dev);
    storage.save();
    return sendJson(res, 200, { ok: true, device: dev });
  }

  if (req.method === "POST" && pathname === "/api/v1/admin/licenses") {
    const body = await readJsonBody(req);
    const id = body.id || `lic_${Date.now()}`;
    const existingIdx = storage.state.licenses.findIndex(
      (l) => l.id === id || (body.licenseKey && l.licenseKey === body.licenseKey)
    );
    const prev = existingIdx >= 0 ? storage.state.licenses[existingIdx] : {};
    const lic = {
      ...prev,
      id: prev.id || id,
      appId: String(body.appId || prev.appId || "global"),
      licenseKey: String(body.licenseKey || prev.licenseKey || `PRNV-${Date.now()}`),
      planTier: String(body.planTier || prev.planTier || "VIP_PRO"),
      status: String(body.status || prev.status || "UNUSED"),
      durationDays: Number(body.durationDays ?? prev.durationDays ?? 30),
      graceDays: Number(body.graceDays ?? prev.graceDays ?? 3),
      paymentVerified: body.paymentVerified !== undefined ? Boolean(body.paymentVerified) : Boolean(prev.paymentVerified),
      transferCooldownHours: Number(body.transferCooldownHours ?? prev.transferCooldownHours ?? config.defaultCooldownHours),
      boundDeviceId: String(body.boundDeviceId ?? prev.boundDeviceId ?? ""),
      expiresAtEpochMs: Number(body.expiresAtEpochMs ?? prev.expiresAtEpochMs ?? 0),
      transferCount: Number(body.transferCount ?? prev.transferCount ?? 0)
    };
    if (existingIdx >= 0) storage.state.licenses[existingIdx] = lic;
    else storage.state.licenses.push(lic);
    storage.save();
    return sendJson(res, 200, { ok: true, license: lic });
  }

  if (req.method === "POST" && pathname === "/api/v1/admin/workspace") {
    const body = await readJsonBody(req);
    storage.state.workspace = {
      ...storage.state.workspace,
      masterMaintenance: Boolean(body.masterMaintenance),
      maintenanceMessage: String(body.maintenanceMessage || storage.state.workspace.maintenanceMessage),
      emergencyLockdown: Boolean(body.emergencyLockdown)
    };
    storage.save();
    return sendJson(res, 200, { ok: true, workspace: storage.state.workspace });
  }

  if (req.method === "POST" && pathname === "/api/v1/admin/events") {
    const body = await readJsonBody(req);
    const evt = storage.appendEvent(
      String(body.category || "OPS"),
      String(body.title || "Admin Event"),
      String(body.message || ""),
      String(body.severity || "INFO"),
      String(body.appId || "global")
    );
    return sendJson(res, 200, { ok: true, event: evt });
  }

  return false;
}
