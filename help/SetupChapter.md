Dependency Management (Setup procedure)
========================
Binary dependencies (Grapes) are loaded from ElectricFlow server through STOMP or (in case Agent and Server are located on the same machine) copied directly from the plugin folder into $COMMANDER_DATA/grape folder.

ElectricFlow Agent should be able to write to $COMMANDER_DATA/grape folder.

In case the plugin procedures are running on a different from local resource, local resource should be available in the system. It can be set through server properties:

    ectool setProperty /server/settings/localResourceName <my local resource name>

In case the local resource is not set, the dependency handling subprocedure will try to find local resource by the following criterias:

* It has a name "local"
* Or its hostname is one of the following: localhost, 127.0.0.1 or &lt;server hostname&gt;.

If neither local resource is found in the resource nor its name is provided in the server settings, the dependencies wil fail to load.

STOMP server should be configured correctly and be available from the Agent resource. STOMP server is using port 61613 by default. See https://helpcenter.electric-cloud.com/hc/en-us/articles/214884463-KBEC-00352-Configuring-Stomp-for-Preflight-and-EC-FileOps-file-transfers for the instructions.

If STOMP server cannot be configured or accessed, one can manually copy dependencies from lib/ folder of the plugin (`$COMMANDER_DATA/@PLUGIN_NAME@/lib`) to `$COMMANDER_DATA/grape` folder.

Failed Setup procedure does not stop the job, so even if it has failed, the overall job still has a chance to finish. If no dependencies are found in the local filesystem, the script itself will attempt to download them from the internet.
