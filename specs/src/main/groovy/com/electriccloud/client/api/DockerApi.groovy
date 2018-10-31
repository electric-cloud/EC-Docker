package com.electriccloud.client.api

import com.electriccloud.client.HttpClient
import de.gesellix.docker.client.DockerClientImpl
import de.gesellix.docker.engine.DockerEnv


class DockerApi extends HttpClient {

    def endpoint
    DockerClientImpl client
    DockerEnv environment

    DockerApi(dockerHost,  certsDir, tlsVerify = false){
        this.environment = new DockerEnv()
        this.environment.dockerHost = dockerHost
        this.environment.tlsVerify = tlsVerify
        this.environment.defaultTlsPort = '2376'
        this.environment.certPath = certsDir
        this.endpoint = "${dockerHost}:${environment.defaultTlsPort}"
        this.client = new DockerClientImpl(environment)
        this.environment.apiVersion = this.client.version().content.ApiVersion
    }


}
