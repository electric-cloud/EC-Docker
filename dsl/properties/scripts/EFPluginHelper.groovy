import com.electriccloud.client.groovy.ElectricFlow

enum EFPluginHelper {
    INSTANCE

    public static EFPluginHelper getInstance() {
        return INSTANCE
    }

    @Lazy
    ElectricFlow ef = { new ElectricFlow() }()

    def getConfigProperties(String configName) {
        assert configName: "No config name is provided"
        ef.getProperties(path: "/plugins/@PLUGIN_KEY@/project/ec_plugin_cfgs/${configName}")?.propertySheet?.property
    }

    Map getConfigPropertiesMap(String configName) {
        assert configName: "No config name is provided"
        getConfigProperties(configName).collectEntries {[it.propertyName, it.value]}
    }

    Credentials getCredentials(def credentialsName) {
        def cred = ef.getFullCredential(credentialName: credentialsName)?.credential
        return new Credentials(userName: (String) cred.userName, password: (String) cred.password)
    }
}
