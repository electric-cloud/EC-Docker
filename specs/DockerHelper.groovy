import spock.lang.*
import com.electriccloud.spec.*
import groovyx.net.http.RESTClient
import groovy.json.JsonBuilder
import static groovyx.net.http.ContentType.JSON
import static groovyx.net.http.Method.DELETE
import static groovyx.net.http.Method.GET
import static groovyx.net.http.Method.PATCH
import static groovyx.net.http.Method.POST
import static groovyx.net.http.Method.PUT


class DockerHelper extends ContainerHelper {
//
//    def createCluster(projectName, envName, clusterName, configName) {
//        createConfig(configName)
//        dsl """
//            project '$projectName', {
//                environment '$envName', {
//                    cluster '$clusterName', {
//                        pluginKey = 'EC-Kubernetes'
//                        provisionParameter = [
//                            config: '$configName'
//                        ]
//                        provisionProcedure = 'Check Cluster'
//                    }
//                }
//            }
//        """
//    }


    def deleteConfig(configName) {
        deleteConfiguration('EC-Kubernetes', configName)
    }

    def createConfig(configName) {
        def endpoint = System.getenv('EC_DOCKER_ENDPOINT')
        assert endpoint
        def pluginConfig = [
            endpoint  : endpoint,
            testConnection   : 'false',
            logLevel         : '2'
        ]
        def props = [:]
        if (System.getenv('RECREATE_CONFIG')) {
            props.recreate = true
        }
        createPluginConfiguration(
            'EC-Docker',
            configName,
            pluginConfig,
            null,
            null,
            props
        )

//
//        if (System.getenv('RECREATE_CONFIG')) {
//            deleteConfiguration('EC-Docker', configName)
//        }
//
//        def result = dsl """
//            runProcedure(
//                projectName: "/plugins/EC-Docker/project",
//                procedureName: 'CreateConfiguration',
//                actualParameter: [
//                    config: '$configName',
//                    endpoint: "$endpoint",
//                    logLevel: '2',
//                    testConnection: "false",
//                    credential: 'credential'
//                ]
//            )
//        """
//
//        assert result?.jobId
//        waitUntil {
//            jobCompleted(result)
//        }
//        assert jobStatus(result.jobId).outcome == 'success'
    }

    def cleanupCluster(configName) {
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
        logger.debug(requestBody)

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


    static def getEndpoint() {
        def endpoint = System.getenv('EF_DOCKER_ENDPOINT')
        assert endpoint
        endpoint
    }
}
