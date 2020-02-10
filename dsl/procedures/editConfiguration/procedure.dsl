import java.io.File

procedure 'EditConfiguration',
    description: 'Edits a previously created configuration for a stand-alone Docker host or Docker Swarm manager', {

    step 'setup',
        subproject: '',
        subprocedure: 'flowpdk-setup',
        command: null,
        errorHandling: 'failProcedure',
        exclusiveMode: 'none',
        postProcessor: '$[/myProject/postpLoader]',
        releaseMode: 'none',
        timeLimitUnits: 'minutes'

    step 'testConnection',
        command: new File(pluginDir, 'dsl/procedures/createConfiguration/steps/testConnection.groovy').text,
        errorHandling: 'abortProcedure',
        condition: '$[testConnection]',
        exclusiveMode: 'none',
        releaseMode: 'none',
        resourceName: '$[grabbedResource]',
        shell: 'ec-groovy',
        timeLimitUnits: 'minutes',
        postProcessor: '$[/myProject/postpLoader]'
}
