$[/myProject/scripts/preamble]

def tempDir = System.getProperty("java.io.tmpdir")
// Create certs dir in temp directory to hold certs
def certDir = new File("${tempDir}/certs")
if(certDir.exists())
{
	def result = certDir.deleteDir() 
	if(!result){
		logger ERROR, "Cleaning up of ${tempDir}/certs directory failed."
		exit 1
	}
}
 