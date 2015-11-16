#
#  Copyright 2015 Electric Cloud, Inc.
#
#  Licensed under the Apache License, Version 2.0 (the "License");
#  you may not use this file except in compliance with the License.
#  You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
#  Unless required by applicable law or agreed to in writing, software
#  distributed under the License is distributed on an "AS IS" BASIS,
#  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
#  See the License for the specific language governing permissions and
#  limitations under the License.
#

@files = (
    ['//property[propertyName="ec_setup"]/value', 'ec_setup.pl'],
	['//step[stepName="runDockerPull"]/command', 'server/docker_pull.pl'],
	['//step[stepName="runDockerBuild"]/command', 'server/docker_build.pl'],
    ['//step[stepName="runDockerRun"]/command', 'server/docker_run.pl'],
	['//procedure[procedureName="runDockerPull"]/propertySheet/property[propertyName="ec_parameterForm"]/value', 'ui_forms/runDockerPull.xml'],
    ['//procedure[procedureName="runDockerBuild"]/propertySheet/property[propertyName="ec_parameterForm"]/value', 'ui_forms/runDockerBuild.xml'],
    ['//procedure[procedureName="runDockerRun"]/propertySheet/property[propertyName="ec_parameterForm"]/value', 'ui_forms/runDockerRun.xml'],
	['//property[propertyName="ec_setup"]/value', 'ec_setup.pl'],
);
