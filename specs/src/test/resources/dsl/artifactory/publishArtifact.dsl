package dsl.artifactory

def names = args.names,
        pluginName             = 'EC-Artifactory',
        artifact               = names.artifact,
        artifactPath           = names.artifactPath,
        artifactProperties     = '',
        classifier             = names.classifier,
        config                 = names.config,
        extension              = names.extension,
        fileItegRev            = 'test',
        folderItegRev          = 'test',
        org                    = names.org,
        orgPath                = names.orgPath,
        repository             = names.repository,
        repositoryLayout       = names.repositoryLayout,
        repositoryPath         = names.repositoryPath,
        repoType               = names.repoType,
        resultPropertySheet    = '',
        type                   = names.type,
        useRepositoryLayout    = 'true',
        version                = names.version

def pluginProjectName = getPlugin(pluginName: pluginName).projectName

runProcedure(
        projectName: pluginProjectName,
        procedureName: 'Publish Artifact',
        actualParameter: [
                artifact: artifact,
                artifactPath: artifactPath,
                artifactProperties: artifactProperties,
                classifier: classifier,
                config: config,
                extension: extension,
                fileItegRev: fileItegRev,
                folderItegRev: folderItegRev,
                org: org,
                orgPath: orgPath,
                repository: repository,
                repositoryLayout: repositoryLayout,
                repositoryPath: repositoryPath,
                repoType: repoType,
                resultPropertySheet: resultPropertySheet,
                type: type,
                useRepositoryLayout: useRepositoryLayout,
                version: version
        ]
)