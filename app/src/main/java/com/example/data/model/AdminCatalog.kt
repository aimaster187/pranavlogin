package com.example.data.model

data class AdminModuleInfo(
    val key: String,
    val title: String,
    val category: String,
    val summary: String
)

object AdminModuleCatalog {
    val dialogTypes = listOf(
        "NORMAL_UPDATE",
        "FORCE_UPDATE",
        "LICENSE_ACTIVATION",
        "VIP_UPGRADE",
        "DEVICE_APPROVAL",
        "CHANNEL_VERIFY",
        "MAINTENANCE",
        "ANNOUNCEMENT",
        "SECURITY_LOCK"
    )

    // Complete 52-module PRANAV Owner Control Catalog with independent toggles
    val allModules: List<AdminModuleInfo> = listOf(
        AdminModuleInfo("owner_auth_guard", "Owner Google Auth Guard", "Security", "Zero-trust single-owner Firebase Auth gate"),
        AdminModuleInfo("core_dashboard_telemetry", "Core Telemetry Dashboard", "Core", "Live KPIs across apps, devices, keys & bots"),
        AdminModuleInfo("multi_app_isolation", "Multi-App Isolation Engine", "Apps", "Per-app isolated configs, keys, dialogs & logs"),
        AdminModuleInfo("original_update_manager", "Original Update Manager", "Updates", "Master/Popup/Force/Later update controls"),
        AdminModuleInfo("staged_rollout_controller", "Staged Rollout Controller", "Updates", "Deterministic percentage rollout per app"),
        AdminModuleInfo("min_version_enforcer", "Minimum Version Enforcer", "Updates", "Hard-block unsupported client builds"),
        AdminModuleInfo("update_confirm_tracker", "Update Confirmation Tracker", "Updates", "Only marks installed after client confirm"),
        AdminModuleInfo("universal_dialog_engine", "Universal Dialog Engine", "Dialogs", "Dynamic JSON dialog rules for all apps"),
        AdminModuleInfo("dialog_animation_fx", "Dialog Motion & SFX Engine", "Dialogs", "Glass slide, pulse & alert sound profiles"),
        AdminModuleInfo("license_key_generator", "License Key Generator", "Licenses", "Single-device cryptographic license issuance"),
        AdminModuleInfo("single_device_binding", "Atomic Single-Device Binding", "Licenses", "Binds license to one Keystore + UUID identity"),
        AdminModuleInfo("vip_payment_verifier", "VIP Payment Verification Gate", "VIP", "Requires verified payment before VIP unlock"),
        AdminModuleInfo("vip_tier_entitlements", "VIP Tier Entitlements", "VIP", "STANDARD, VIP_PRO & VIP_ELITE policies"),
        AdminModuleInfo("grace_period_manager", "Grace Period Manager", "Licenses", "Configurable post-expiry grace windows"),
        AdminModuleInfo("license_transfer_cooldown", "Transfer Cooldown & Limit", "Licenses", "Prevents key sharing & transfer abuse"),
        AdminModuleInfo("device_keystore_identity", "Keystore EC P-256 Identity", "Devices", "Hardware-backed identity without IMEI/MAC"),
        AdminModuleInfo("device_challenge_auth", "Challenge-Response Nonce Auth", "Devices", "Signed nonce verification against replay"),
        AdminModuleInfo("device_approval_queue", "Device Approval Workflow", "Devices", "Approve, reset pending, or block devices"),
        AdminModuleInfo("risk_scoring_engine", "Device Risk & Anomaly Engine", "Security", "Tracks anomaly score & multi-device abuse"),
        AdminModuleInfo("emergency_kill_switch", "Emergency Global Kill-Switch", "Security", "Instant lockdown across all managed APKs"),
        AdminModuleInfo("global_maintenance_switch", "Global Maintenance Mode", "Core", "Graceful maintenance dialog for non-owners"),
        AdminModuleInfo("per_app_maintenance", "Per-App Maintenance Mode", "Apps", "Independent maintenance lock per package"),
        AdminModuleInfo("dex_release_manager", "PRANAV DEX Release Manager", "DEX", "Versioned DEX builds with SHA-256 digests"),
        AdminModuleInfo("dex_init_guard", "Duplicate-Init Prevention Guard", "DEX", "Atomic once-per-process SDK initialization"),
        AdminModuleInfo("app_lifecycle_hook", "Application Lifecycle Hook", "Hooks", "Separate Application.onCreate integration"),
        AdminModuleInfo("activity_lifecycle_hook", "Activity Lifecycle Hook", "Hooks", "Separate Activity.onCreate integration"),
        AdminModuleInfo("attach_base_context_hook", "attachBaseContext Hook", "Hooks", "Early-stage multidex-safe init hook"),
        AdminModuleInfo("dex_dynamic_loader", "Verified DEX Loader Helper", "Hooks", "Checksum-verified DEX loading & rollback"),
        AdminModuleInfo("apk_saf_inspector", "SAF Authorized APK Inspector", "Integration", "Parses AXML manifest & classes*.dex locally"),
        AdminModuleInfo("compatibility_diagnostics", "APK Compatibility Diagnostics", "Integration", "Detects existing hooks, minSdk & split APKs"),
        AdminModuleInfo("mt_manager_smali_gen", "MT Manager Smali Generator", "Integration", "Generates exact invoke-static hooks"),
        AdminModuleInfo("rollback_recovery_kit", "Integration Rollback Kit", "Integration", "Clean hook removal & backup restore guidance"),
        AdminModuleInfo("telegram_multi_bot_hub", "Dynamic Telegram Multi-Bot Hub", "Bots", "Unlimited isolated User & Owner bots"),
        AdminModuleInfo("telegram_getme_verifier", "Telegram getMe Validator", "Bots", "Verifies live bot identity before saving"),
        AdminModuleInfo("encrypted_token_vault", "AES-256-GCM Token Vault", "Bots", "Encrypted server-side bot token storage"),
        AdminModuleInfo("bot_start_continue_menu", "Start -> CONTINUE -> Main Menu", "Bots", "Interactive inline callback menu flow"),
        AdminModuleInfo("user_bot_14_commands", "User Bot 14-Command Suite", "Bots", "License, VIP, status, transfer & support"),
        AdminModuleInfo("owner_bot_25_commands", "Owner Bot 25-Command Suite", "Bots", "Remote ops, bans, broadcasts & health"),
        AdminModuleInfo("bot_rate_limiter", "Per-Bot Flood & Rate Limiter", "Bots", "Protects Telegram API & VPS worker pool"),
        AdminModuleInfo("webhook_polling_router", "Webhook & Polling Router", "Bots", "Auto-switches polling vs HTTPS webhook"),
        AdminModuleInfo("announcement_broadcaster", "Announcement Broadcaster", "Comms", "Push structured notices to target apps"),
        AdminModuleInfo("support_ticket_desk", "Support Ticket Queue", "Comms", "Track & resolve user support tickets"),
        AdminModuleInfo("audit_event_stream", "Immutable Ops Audit Log", "Comms", "Records every owner & client security event"),
        AdminModuleInfo("channel_verification_gate", "Channel Join Verification", "Comms", "Enforces required channel membership dialogs"),
        AdminModuleInfo("vps_ram_cpu_watchdog", "VPS RAM & CPU Watchdog", "Infra", "Real /proc & os metrics monitoring"),
        AdminModuleInfo("expiry_cron_scheduler", "License Expiry Worker", "Infra", "Automated expiry, grace & webhook checks"),
        AdminModuleInfo("subdomain_routing_config", "Subdomain Nginx Router", "Infra", "admin/api/bot.pranavsmm.shop routing"),
        AdminModuleInfo("ssl_certbot_automation", "TLS & Certbot Provisioner", "Infra", "HTTPS reverse-proxy hardening scripts"),
        AdminModuleInfo("offline_room_cache", "Offline Room Cache Sync", "Offline", "Caches snapshots safely for offline review"),
        AdminModuleInfo("low_end_animation_fallback", "Low-End Animation Fallback", "UI", "Reduces GPU blur & orb pulse overhead"),
        AdminModuleInfo("zero_trust_firestore_rules", "Zero-Trust Firestore Rules", "Security", "Strict ownerUid == auth.uid isolation"),
        AdminModuleInfo("automated_backup_snapshot", "Data Backup & Restore Script", "Infra", "Scheduled encrypted backup archive tool")
    )
}
