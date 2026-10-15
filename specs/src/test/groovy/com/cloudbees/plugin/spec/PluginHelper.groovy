package com.cloudbees.plugin.spec

import com.electriccloud.spec.PluginSpockTestSupport
import spock.lang.Shared

class PluginHelper extends PluginSpockTestSupport{

    static PLUGIN_NAME = 'EC-Docker'
    static final String DOCKER_HUB_HOST = 'https://registry-1.docker.io/v2/'

    @Lazy
    static String EC_DOCKERHUB_USER = { return getCheckedVariable('EC_DOCKERHUB_USER') }()

    @Lazy
    static String EC_DOCKERHUB_PASSWORD= { return getCheckedVariable('EC_DOCKERHUB_PASSWORD') }()

    static String getCheckedVariable(String variableName) {
        def value = System.getenv(variableName)
        if (!value) {
            throw new RuntimeException("Environment variable '${variableName}' does not have a value")
        }
        return value
    }
}
