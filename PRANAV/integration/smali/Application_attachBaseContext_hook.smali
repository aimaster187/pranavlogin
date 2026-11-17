# PRANAV Hook Template — Application.attachBaseContext(Context)
# Use for multidex / early-lifecycle apps where Application.onCreate is already crowded.

.method protected attachBaseContext(Landroid/content/Context;)V
    .locals 3

    invoke-super {p0, p1}, Landroid/app/Application;->attachBaseContext(Landroid/content/Context;)V

    const-string v0, "my_app_code"
    const-string v1, "https://api.pranavsmm.shop"

    invoke-static {p0, p1, v0, v1}, Lcom/pranav/integration/hook/PranavAttachBaseContextHook;->attachEarly(Landroid/app/Application;Landroid/content/Context;Ljava/lang/String;Ljava/lang/String;)V

    return-void
.end method
