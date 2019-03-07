package com.electriccloud.specs

import spock.lang.*
import com.electriccloud.spec.*
import groovyx.net.http.RESTClient
import groovy.json.JsonBuilder
import static groovyx.net.http.ContentType.JSON
import static groovyx.net.http.Method.*


class DockerHelper extends ContainerHelper {

    static String PLUGIN_NAME = 'EC-Docker'
    static String pluginVersion = System.getenv('PLUGIN_VERSION') ?: ''

    @Shared
    def certsPath = 'src/test/resources/certs',
        configurationName = "dockerConfig",
        configSwarm = "dockerConfigSwarm",
        credentialLogin = getCommanderLogin(),
        credentialPassword = getCommanderPassword(),

    createConfigParams = [
             config         : '',
             desc           : '',
             endpoint       : '',
             cacert         : '',
             cert           : '',
             credential     : '',
             logLevel       : '',
             testConnection : ''
    ],
    createIngressParams = [
             pluginConfig : configSwarm,
             networkName  : '',
             subnetList   : '',
             gatewayList  : '',
             enableIpv6   : '',
             mtu          : '',
             labels       : ''
    ],
    deleteNetworkParams = [
             pluginConfig : configSwarm,
             networkName  : ''
    ],
    deleteConfigParams = [
            config : ''
    ]

    static String getAssertedEnvVariable(String varName) {
        String varValue = System.getenv(varName)
        assert varValue
        return varValue
    }

    static String getECDockerEndpoint() { getAssertedEnvVariable("EC_DOCKER_ENDPOINT") }

    static String getCommanderHost() { getAssertedEnvVariable("COMMANDER_HOST") }

    static String getCommanderLogin() { getAssertedEnvVariable("COMMANDER_LOGIN") }

    static String getCommanderPassword() { getAssertedEnvVariable("COMMANDER_PASSWORD") }

    static String getCommanderWorkspace() { getAssertedEnvVariable("COMMANDER_WORKSPACE") }

    static String getESToolHome() { getAssertedEnvVariable("ECTOOL_HOME") }

    static String getDockerCommunityEndpoint() { getAssertedEnvVariable("DOCKER_COMMUNITY_ENDPOINT") }

    static String getDockerTLSEndpoint() { getAssertedEnvVariable("DOCKER_TLS_ENDPOINT") }

    static String getDockerSwarmEndpoint() { getAssertedEnvVariable("DOCKER_SWARM_ENDPOINT") }

    static String getDockerSwarmNodeEndpoint() { getAssertedEnvVariable("DOCKER_SWARM_NODE_ENDPOINT") }

    static String getDockerCACert() { getAssertedEnvVariable("DOCKER_CA_CERT") }

    static String getDockerCert() { getAssertedEnvVariable("DOCKER_CERT") }

    static String getDockerKey() { getAssertedEnvVariable("DOCKER_KEY") }

    static String getDockerHubEndpoint() { getAssertedEnvVariable("DOCKER_HUB_ENDPOINT") }

    static String getDockerHubID() { getAssertedEnvVariable("DOCKER_HUB_ID") }

    static String getDockerHubPassword() { getAssertedEnvVariable("DOCKER_HUB_PASSWORD") }

    static String getArtifactoryAdminUsername() { getAssertedEnvVariable("ARTIFACTORY_ADMIN_USERNAME") }

    static String getArtifactoryAdminPassword() { getAssertedEnvVariable("ARTIFACTORY_ADMIN_PASSWORD") }

    def createCluster(projectName, envName, clusterName, String configName) {
        createConfig(configName)
        dsl """
            project '$projectName', {
                environment '$envName', {
                    cluster '$clusterName', {
                        pluginKey = 'EC-Docker'
                        provisionParameter = [
                            config: '$configName'
                        ]
                        provisionProcedure = 'Check Cluster'
                    }
                }
            }
        """
   }


    def deleteConfig(String configName = configurationName) {
        deleteConfiguration(PLUGIN_NAME, configName)
    }

    def createConfig(String configName = configurationName) {
        def pluginConfig = [
            endpoint  : getECDockerEndpoint(),
            testConnection: 'false',
            logLevel: '1'
        ]
        def props = [:]
        if (System.getenv('RECREATE_CONFIG')) {
            props.recreate = true
        }
        createPluginConfiguration(
                PLUGIN_NAME,
            configName,
            pluginConfig,
            null,
            null,
            props
        )
    }

    def createConfig(String configName, endpoint ) {
        def pluginConfig = [
                endpoint  : endpoint,
                testConnection: 'false',
                logLevel: '1'
        ]
        def props = [:]
        if (System.getenv('RECREATE_CONFIG')) {
            props.recreate = true
        }
        createPluginConfiguration(
                PLUGIN_NAME,
                configName,
                pluginConfig,
                null,
                null,
                props
        )
    }

    def cleanupCluster( configName = configurationName) {
        assert configName
        def procName = 'Cleanup Cluster - Experimental'
        def result = dsl """
            runProcedure(
                projectName: '/plugins/EC-Docker/project',
                procedureName: "$procName",
                actualParameter: [
                    namespace: 'default',
                    config: '$configName'
                ]
            )
        """
        assert result.jobId

        def time = 0
        def timeout = 300
        def delay = 50
        while (jobStatus(result.jobId).status != 'completed' && time < timeout) {
            sleep(delay * 1000)
            time += delay
        }

        jobCompleted(result)
    }

    static def request(requestUrl, requestUri, method, queryArgs, requestHeaders, requestBody) {
        def http = new RESTClient(requestUrl)
        http.ignoreSSLIssues()
        logger.debug (requestBody)

        http.request(method, JSON) {
            if (requestUri) {
                uri.path = requestUri
            }
            logger.debug(uri.path)
            logger.debug(method.toString())
            if (queryArgs) {
                uri.query = queryArgs
            }
            headers = requestHeaders
            body = requestBody

            response.success = { resp, json ->
                [statusLine: resp.statusLine,
                 status    : resp.status,
                 data      : json]
            }

            response.failure = { resp, reader ->
                println resp
                println reader
                throw new RuntimeException("Request failed")
            }

        }
    }

    static def createService(endpoint = getECDockerEndpoint(), token, payload) {
        def namespace = 'default'
        def uri = "/api/v1/namespaces/${namespace}/services"
        request(endpoint, uri, POST, null, ["Authorization": "Bearer ${getToken()}"], new JsonBuilder(payload).toPrettyString())
    }


    static def getService(name) {
        def uri = "/api/v1/namespaces/default/services/${name}"
        request(
                getECDockerEndpoint(), uri, GET,
                null, ["Authorization": "Bearer ${getToken()}"], null).data
    }

    static def getDeployment(name) {
        def uri = "/apis/apps/v1beta1/namespaces/default/deployments/${name}"
        request(
                getECDockerEndpoint(), uri, GET,
                null, ["Authorization": "Bearer ${getToken()}"], null).data
    }

    static def deleteService(serviceName) {
        def uri = "/api/v1/namespaces/default/services/$serviceName"
        request(getECDockerEndpoint(), uri, DELETE, null, ["Authorization": "Bearer ${getToken()}"], null)
    }


    def runTestedProcedure(String projectName, String procedureName, String resourceName = 'local', Map params) {

        dslFile('dsl/RunProcedure.dsl', [
                projectName  : projectName,
                procedureName: procedureName,
                resourceName : resourceName,
                params       : params
        ])

        // Stringify map
        def params_str_arr = []
        params.each() { k, v ->
            params_str_arr.push(k + " : '''" + (v ?: '') + "'''")
        }
        logger.debug("Parameters string: " + params_str_arr.toString())

        String procedureDsl = """
            runProcedure(
                projectName: '$projectName',
                procedureName: '$procedureName',
                actualParameter: $params_str_arr
            )
                """

        def result = runProcedure( (String) procedureDsl, resourceName,
                180, // timeout
        )
        return result
    }

    def getJobUpperStepSummary(def jobId) {
        assert jobId
        def summary = null
        def property = "/myJob/jobSteps/RunProcedure/summary"
        println "Trying to get the summary, property: $property, jobId: $jobId"
        try {
            summary = getJobProperty(property, jobId)
        } catch (Throwable e) {
            logger.error("Can't retrieve Upper Step Summary from the property: '$property'; check job: " + jobId)
        }
        return summary
    }

    def conditionallyDeleteProject(String projectName) {
        if (System.getenv("LEAVE_TEST_PROJECTS")) {
            return
        }
        dsl "deleteProject(projectName: '$projectName')"
    }

    class DockerHubClient {
        def token
        def username
        static final def DOCKERHUB_URL = "https://hub.docker.com"


        DockerHubClient(username, password) {
            this.token = auth(username, password)
            this.username = username
        }


        def listRepositories() {
            def data = dockerhubRequest("/v2/repositories/${username}")
            data
        }

        def deleteRepository(name) {
            dockerhubRequest("/v2/repositories/${username}/${name}", DELETE)
        }

        def getRepository(name) {
            dockerhubRequest("/v2/repositories/${username}/${name}")
        }

        static def auth(username, password) {
            def body = """{"username": "$username", "password": "$password"}"""
            def data = request(DOCKERHUB_URL, "/v2/users/login/", POST, null, ["Content-Type": "application/json"], body)?.data
            assert data
            assert data.token
            data.token
        }

        def dockerhubRequest(uri, method = GET,  headers = [:], query = [:], body = null) {
            headers["Authorization"] = "JWT ${token}"
            request(DOCKERHUB_URL, uri, method, query, headers, body)?.data
        }

        static def request(requestUrl, requestUri, method, queryArgs, requestHeaders, requestBody) {
            def http = new RESTClient(requestUrl)
            http.ignoreSSLIssues()

            http.request(method, JSON) {
                if (requestUri) {
                    uri.path = requestUri
                }
                if (queryArgs) {
                    uri.query = queryArgs
                }
                headers = requestHeaders
                body = requestBody

                response.success = { resp, json ->
                    [statusLine: resp.statusLine,
                     status    : resp.status,
                     data      : json]
                }

                response.failure = { resp, reader ->
                    println resp
                    println reader
                    throw new RuntimeException("Request failed")
                }

            }
        }
    }

}
