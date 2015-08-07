@files = (
    ['//property[propertyName="ec_setup"]/value', 'ec_setup.pl'],
	['//step[stepName="runDockerPull"]/command', 'server/docker_pull.pl'],
	['//step[stepName="runDockerBuild"]/command', 'server/docker_build.pl'],
    ['//step[stepName="runDockerRun"]/command', 'server/docker_run.pl'],
	['//step[stepName="runDockerComposeUp"]/command', 'server/docker_compose_up.pl'],
	['//procedure[procedureName="runDockerPull"]/propertySheet/property[propertyName="ec_parameterForm"]/value', 'ui_forms/runDockerPull.xml'],
    ['//procedure[procedureName="runDockerBuild"]/propertySheet/property[propertyName="ec_parameterForm"]/value', 'ui_forms/runDockerBuild.xml'],
    ['//procedure[procedureName="runDockerRun"]/propertySheet/property[propertyName="ec_parameterForm"]/value', 'ui_forms/runDockerRun.xml'],
	['//procedure[procedureName="runDockerComposeUp"]/propertySheet/property[propertyName="ec_parameterForm"]/value', 'ui_forms/runDockerComposeUp.xml'],
	['//property[propertyName="ec_setup"]/value', 'ec_setup.pl'],
);
