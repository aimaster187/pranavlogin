# PRANAV Hook Template — MainActivity.onCreate(Bundle)
# Use this ONLY when the host APK has no custom Application class and you do not want to create one.

.method protected onCreate(Landroid/os/Bundle;)V
    .locals 2

    invoke-super {p0, p1}, Landroid/app/Activity;->onCreate(Landroid/os/Bundle;)V

    const-string v0, "my_app_code"
    const-string v1, "https://api.pranavsmm.shop"

    invoke-static {p0, v0, v1}, Lcom/pranav/integration/hook/PranavActivityHook;->onActivityCreated(Landroid/app/Activity;Ljava/lang/String;Ljava/lang/String;)V

    return-void
.end method
