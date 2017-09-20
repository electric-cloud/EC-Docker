import java.io.File

procedure 'Delete Network',
    description: 'Deletes a network', {
    
    step 'deleteNetwork',
      command: new File(pluginDir, 'dsl/procedures/deleteNetwork/steps/deleteNetwork.groovy').text,
      errorHandling: 'abortProcedure',
      exclusiveMode: 'none',
      releaseMode: 'none',
      shell: 'ec-groovy',
      timeLimitUnits: 'minutes'
   }