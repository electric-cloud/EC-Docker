import java.io.File

procedure 'Delete Network',
    description: 'Deletes a network', {

    step 'setup',
      subproject: '',
      subprocedure: 'flowpdk-setup',
      command: null,
      errorHandling: 'failProcedure',
      exclusiveMode: 'none',
      postProcessor: 'postp',
      releaseMode: 'none',
      timeLimitUnits: 'minutes'

    step 'deleteNetwork',
      command: new File(pluginDir, 'dsl/procedures/deleteNetwork/steps/deleteNetwork.groovy').text,
      errorHandling: 'abortProcedure',
      exclusiveMode: 'none',
      releaseMode: 'none',
      resourceName: '$[grabbedResource]',
      shell: 'ec-groovy',
      timeLimitUnits: 'minutes'
   }
