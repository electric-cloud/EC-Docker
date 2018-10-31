package dsl

def params = args.params
def projName = args.projName

project projName, {
    credential 'dockerhub', {
        userName = args.dockerhubUsername
        password = args.dockerhubPassword
    }

    procedure 'war to image', {

        params.each { k, v ->
            def defaultValue = v ?: ''
            formalParameter k, defaultValue: defaultValue, {
                type = 'textarea'
            }
        }

        step 'Artifact 2 Image', {
            subproject = '/plugins/EC-Docker/project'
            subprocedure = 'com.electriccloud.specs.Artifact2Image'

            params.each { k, v ->
                actualParameter k, '$[' + k + ']'
            }
            actualParameter 'ecp_docker_credential', 'dockerhub'
            attachedCredentials ["dockerhub"]
//            attachedCredential ["/projects/$projName/credential/dockerhub"]
//            attachedParameter ["dockerhub"]
        }
    }
}

