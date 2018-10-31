package com.electriccloud.client.api

import com.electriccloud.client.HttpClient
import io.restassured.RestAssured
import io.restassured.filter.log.RequestLoggingFilter
import io.restassured.filter.log.ResponseLoggingFilter
import io.restassured.http.ContentType

import static groovyx.net.http.Method.*

class DockerHubApi extends HttpClient {


    def token
    def username
    def password
    def baseUrl = "https://hub.docker.com"
    def defaultHeaders = { [Authorization: "JWT ${token}", Accept: "application/json"] }

    DockerHubApi(dockerId, dockerPass){
        log.info("Connecting to $baseUrl")
        log.info("As user: dockerId = ${dockerId}")
        this.username = dockerId
        this.password = dockerPass
        this.token = auth(username, password).json.token
    }

    def auth(username, password){
        def body = """{"username": "$username", "password": "$password"}"""
        request(baseUrl, 'v2/users/login/', POST, body, null , null, true)
    }

    def getRepositories() {
        def resp = request(baseUrl, "v2/repositories/${username}/", GET, null, defaultHeaders(), null, true)
        resp
    }

    def deleteRepository(name) {
        try {
            request(baseUrl, "v2/repositories/${username}/${name}/", DELETE, null, defaultHeaders(), [:], true)
        } catch (e){
            assert e.message == 'ACCEPTED'
            log.info("Repository: ${username}/${name} is successfully removed.")
        }
        return this
    }

    def getRepository(name) {
        def resp = request(baseUrl, "v2/repositories/${username}/${name}/", GET, null, defaultHeaders(), null, true)
        resp
    }

    def getRepo(name){
        return RestAssured.given().relaxedHTTPSValidation() //.filters(new RequestLoggingFilter(), new ResponseLoggingFilter())
                .header("Authorization", "JWT ${token}")
                .contentType(ContentType.JSON)
                .when().get("${baseUrl}/v2/repositories/${username}/${name}/")
    }


}
