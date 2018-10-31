package dsl

project args.projectName, {
    procedure "Read Dockerfile", {
        step 'Read Dockerfile', {
            shell = 'ec-groovy'
            command = '''
import com.electriccloud.client.groovy.ElectricFlow

def artifactName = '\$[artifactName]'
def path = '\$[path]'
def dockerfile = new File(path + '/' + artifactName + '-dockerfile', 'Dockerfile')
def ef = new ElectricFlow()
def content = dockerfile.text
ef.setProperty(propertyName: '/myJob/dockerfile', value: content)

'''
        }

        formalParameter 'artifactName', defaultValue: '', {
            type = 'entry'
        }

        formalParameter 'path', defaultValue: '', {
            type = 'entry'
        }
    }
}