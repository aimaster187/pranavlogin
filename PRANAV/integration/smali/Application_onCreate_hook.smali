# PRANAV Hook Template — Application.onCreate()
# Insert inside your host Application subclass's onCreate() method right after invoke-super.

.method public onCreate()V
    .locals 3

    invoke-super {p0}, Landroid/app/Application;->onCreate()V

    # Replace with your App Code from PRANAV Admin -> Apps & Updates
    const-string v0, "my_app_code"
    # Replace with your PRANAV API domain endpoint
    const-string v1, "https://api.pranavsmm.shop"

    invoke-static {p0, v0, v1}, Lcom/pranav/integration/hook/PranavApplicationHook;->attach(Landroid/app/Application;Ljava/lang/String;Ljava/lang/String;)V

    return-void
.end method
