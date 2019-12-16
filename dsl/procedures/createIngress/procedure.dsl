import java.io.File

procedure 'Create Ingress',
    description: 'Configures default ingress network in Docker Swarm cluster', {

    step 'setup',
      subproject: '',
      subprocedure: 'flowpdk-setup',
      command: null,
      errorHandling: 'failProcedure',
      exclusiveMode: 'none',
      postProcessor: 'postp',
      releaseMode: 'none',
      timeLimitUnits: 'minutes'

    step 'createIngress',
      command: new File(pluginDir, 'dsl/procedures/createIngress/steps/createIngress.groovy').text,
      errorHandling: 'abortProcedure',
      exclusiveMode: 'none',
      releaseMode: 'none',
      shell: 'ec-groovy',
      resourceName: '$[grabbedResource]',
      timeLimitUnits: 'minutes'
   }
