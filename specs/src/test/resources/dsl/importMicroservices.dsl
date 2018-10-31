package dsl

def projName = args.projectName
def params = args.params

project projName, {
  procedure 'Import Microservices', {
    params.each {k, v ->
      formalParameter k, defaultValue: '', {
        type = 'textarea'
      }
    }
    step 'Import Microservices', {
      subproject = '/plugins/EC-Docker/project'
      subprocedure = 'Import Microservices'

      params.each { k, v ->
        actualParameter k, value: '$[' + k + ']'
      }
    }
  }
}
