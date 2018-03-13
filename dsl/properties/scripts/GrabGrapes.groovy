@Grapes([
    @Grab("de.gesellix:docker-client:2018-01-26T21-28-05"),
    @Grab(group='ch.qos.logback', module='logback-classic', version='1.0.13'),
    @GrabExclude(group='org.codehaus.groovy', module='groovy', version='2.4.11'),
    @Grab(group='net.sf.json-lib', module='json-lib', version='2.3', classifier='jdk15'),
    @Grab('org.codehaus.groovy.modules.http-builder:http-builder:0.7.1')
])