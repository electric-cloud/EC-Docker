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
}
