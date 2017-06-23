/**
 * Docker API client
 */
public class DockerClient extends BaseClient {


   /*	Returns string "reachable" or "unreachable" based on availability of
    *   docker engine (if swarmMode is false) 
   	*   docker swarm manager (if swarmMode is true)
	*
	*/
	def checkHealth(String endpoint, boolean swarmMode){

			if (OFFLINE) return null
			def response 
			def reachability
			if (swarmMode) {
				response = doHttpRequest(GET, endpoint, "/nodes", [:], /*failOnErrorCode*/ true, /*requestBody*/ null)
				reachability = response.data[0].ManagerStatus.Reachability

			} else {
				response = doHttpRequest(GET, endpoint, "/info", [:], /*failOnErrorCode*/ true, /*requestBody*/ null)
				if(response.status == 200){
					reachability = "reachable"
				}else{
					reachability = "unreachable"
				}
				
			}

	}


}