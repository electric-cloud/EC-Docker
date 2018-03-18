

class SampleSpec extends DockerHelper {
    def "sample"() {
        when:
        assert 1 == 1
        println "hello"

        println this.getClass().getResource("/resources/test").text
        File helloWorldWar = new File(this.getClass().getResource("/resources/hello-world.war").toURI())
        String path = helloWorldWar.absolutePath
        println path

        String commanderServer = System.getProperty("COMMANDER_SERVER") ?: 'localhost'

        def artifactName = "ec-specs:hello-world-war"
        def version = "1.0.0"
        "ectool login --server $commanderServer admin changeme".execute().waitFor()
        "ectool --server $commanderServer deleteArtifactVersion ${artifactName}:${version}".execute().waitFor()
        def process = "ectool --server $commanderServer publishArtifactVersion --version ${version} --artifactName $artifactName --fromDirectory ${helloWorldWar.parentFile} --includePatterns hello-world.war".execute()
        process.waitFor()
        println process.text
        then:
        assert true
    }
}
