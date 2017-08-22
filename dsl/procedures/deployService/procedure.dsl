import java.io.File

procedure 'Deploy Service',
	description: 'Deploys or updates a service on a stand-alone Docker host or a Docker Swarm cluster', {

	step 'createOrUpdateDeployment',
	  command: new File(pluginDir, 'dsl/procedures/deployService/steps/createOrUpdateDeployment.groovy').text,
	  errorHandling: 'failProcedure',
	  exclusiveMode: 'none',
	  postProcessor: 'postp',
	  releaseMode: 'none',
	  shell: 'ec-groovy',
	  timeLimitUnits: 'minutes'
	
}