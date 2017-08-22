import java.io.File

procedure 'Undeploy Service',
	description: 'Undeploys a previously deployed service on a stand-alone Docker host or a Docker Swarm cluster', {

	step 'undeployService',
	  command: new File(pluginDir, 'dsl/procedures/undeployService/steps/undeployService.groovy').text,
	  errorHandling: 'failProcedure',
	  exclusiveMode: 'none',
	  postProcessor: 'postp',
	  releaseMode: 'none',
	  shell: 'ec-groovy',
	  timeLimitUnits: 'minutes'

}