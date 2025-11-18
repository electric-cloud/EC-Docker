package com.cloudbees.plugin.spec

import com.electriccloud.spec.PluginSpockTestSupport
import spock.lang.Shared

class PluginHelper extends PluginSpockTestSupport{

    static PLUGIN_NAME = 'EC-Docker'
    static final String DOCKER_HUB_HOST = 'https://registry-1.docker.io/v2/'

    @Lazy
    static String DOCKERHUB_SPECS_USERNAME = { return getCheckedVariable('DOCKERHUB_SPECS_USERNAME') }()

    @Lazy
    static String DOCKERHUB_SPECS_PASSWORD= { return getCheckedVariable('DOCKERHUB_SPECS_PASSWORD') }()

    static String getCheckedVariable(String variableName) {
        def value = System.getenv(variableName)
        if (!value) {
            throw new RuntimeException("Environment variable '${variableName}' does not have a value")
        }
        return value
    }

    def getPlugin(def pluginName) {
        def result = dsl """
                        getPlugin(pluginName: '$pluginName')
                    """
        return result
    }

    /**
     * Parses the plugin info map and returns ONLY the version string.
     *
     * @return The version string (e.g., "2.0.1.2025011751") if found,
     * @throws RuntimeException if the pluginVersion field is missing, empty,
     * or does not contain a valid version.
     */
    def getPluginVersion() {
        Map pluginInfo = getPlugin(PLUGIN_NAME)
        print("Plugin Info: $pluginInfo\n")

        def versionStr = pluginInfo?.plugin?.pluginVersion?.toString()

        if (versionStr == null || versionStr.trim().empty) {
            throw new RuntimeException("Invalid Response: 'plugin.pluginVersion' field is missing or empty.")
        }

        return versionStr.trim()
    }
    /**
     * Helper method to compare semantic versions
     * @param current Current version string (e.g., "2.0.1.2025011751")
     * @param minimum Minimum required version (e.g., "2.0.0")
     * @return true if current >= minimum
     */
    protected boolean isVersionAtLeast(String current, String minimum) {
        def currentParts = current.tokenize('.').collect { it.toInteger() }
        def minimumParts = minimum.tokenize('.').collect { it.toInteger() }

        for (int i = 0; i < Math.min(currentParts.size(), minimumParts.size()); i++) {
            if (currentParts[i] > minimumParts[i]) return true
            if (currentParts[i] < minimumParts[i]) return false
        }
        return currentParts.size() >= minimumParts.size()
    }
}
