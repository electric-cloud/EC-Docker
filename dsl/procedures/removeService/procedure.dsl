import java.io.File

procedure 'Remove Docker Service',
  description: 'Removes service deployed on a stand-alone Docker host or a Docker Swarm cluster.', {

	step 'setup',
      subproject: '',
      subprocedure: 'flowpdk-setup',
      command: null,
      errorHandling: 'failProcedure',
      exclusiveMode: 'none',
      postProcessor: 'postp',
      releaseMode: 'none',
      timeLimitUnits: 'minutes', {

    	  actualParameter 'additionalArtifactVersion', ''
    }

	step 'cleanup',
	  command: new File(pluginDir, 'dsl/procedures/removeService/steps/removeService.groovy').text,
	  errorHandling: 'failProcedure',
	  exclusiveMode: 'none',
	  postProcessor: 'postp',
	  releaseMode: 'none',
	  shell: 'ec-groovy',
	  resourceName: '$[grabbedResource]',
	  timeLimitUnits: 'minutes'

}

