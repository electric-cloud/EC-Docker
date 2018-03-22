import java.io.File

procedure 'Create Ingress',
    description: 'Configures default ingress network in Docker Swarm cluster', {
    
    step 'setup',
      subproject: '',
      subprocedure: 'Setup',
      command: null,
      errorHandling: 'failProcedure',
      exclusiveMode: 'call',
      postProcessor: 'postp',
      releaseMode: 'none',
      timeLimitUnits: 'minutes', {

    	  actualParameter 'additionalArtifactVersion', ''
    }
    
    step 'createIngress',
      command: new File(pluginDir, 'dsl/procedures/createIngress/steps/createIngress.groovy').text,
      errorHandling: 'abortProcedure',
      exclusiveMode: 'none',
      releaseMode: 'none',
      shell: 'ec-groovy',
      timeLimitUnits: 'minutes'
   }