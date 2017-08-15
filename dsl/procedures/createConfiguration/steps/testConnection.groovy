@Grab("de.gesellix:docker-client:2017-06-25T15-38-14")
@Grab(group='ch.qos.logback', module='logback-classic', version='1.0.13')

import de.gesellix.docker.client.DockerClientImpl

$[/myProject/scripts/preamble]

EFClient efClient = new EFClient()
def actualParams = efClient.getActualParameters()

if (efClient.toBoolean(actualParams.get('testConnection'))) {

	def tempDir = System.getProperty("java.io.tmpdir")
	def certDir = new File("${tempDir}/certs")

	if(!certDir.exists())
	{
		certDir.mkdir() 
	}
	def cacert = actualParams.get('cacert')
    if (cacert){
    	File cacertFile = new File("${tempDir}/certs/ca.pem")
		cacertFile.text = cacert
    }
    def cert = actualParams.get('cert')
    if (cert){
    	File certFile = new File("${tempDir}/certs/cert.pem")
		certFile.text = cert
    }
    def key = efClient.getCredentials('credential')
    if (key.password){
    	File keyFile = new File("${tempDir}/certs/key.pem")
		keyFile.text = key.password
		System.setProperty("docker.tls.verify", "1")
		System.setProperty("docker.cert.path","${tempDir}/certs")
    }

	def endpoint = actualParams.get('endpoint')
    try{
		def dockerClient = new DockerClientImpl(endpoint)
		dockerClient.info().content
    }catch(Exception e){
        e.printStackTrace()
    	efClient.handleConfigurationError("Error while connecting to docker endpoint ${endpoint} : ${e.getMessage()}")
    }
}