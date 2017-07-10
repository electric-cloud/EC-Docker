@Grab("de.gesellix:docker-client:2017-06-25T15-38-14")
@Grab(group='ch.qos.logback', module='logback-classic', version='1.0.13')

import de.gesellix.docker.client.image.ManageImage
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
	def cacert = efClient.getCredentials('cacert')
    if (cacert.password){
    	File cacertFile = new File("${tempDir}/certs/ca.pem")
		cacertFile.text = cacert.password
    }
    def cert = efClient.getCredentials('cert')
    if (cert.password){
    	File certFile = new File("${tempDir}/certs/cert.pem")
		certFile.text = cert.password
    }
    def key = efClient.getCredentials('key')
    if (key.password){
    	File keyFile = new File("${tempDir}/certs/key.pem")
		keyFile.text = key.password
		System.setProperty("docker.tls.verify", "1")
		System.setProperty("docker.cert.path","${tempDir}/certs")
    }

    def endpoint = actualParams.get('endpoint')
    dockerClient = new DockerClientImpl()
    try{
    	def info = dockerClient.info(endpoint).content
    }catch(Exception e){
    	logger ERROR, "${e}"
        logger ERROR, "${endpoint} is not Swarm Manager. Exiting.."
        exit 1
    }finally{
    	if(certDir.exists())
		{
			def result = certDir.deleteDir() 
			if(!result){
				logger ERROR, "Cleaning up of ${tempDir}/certs directory failed."
				exit 1
			}
		}
    }
}