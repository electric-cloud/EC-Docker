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

use strict;
use utf8;
use ElectricCommander;

$| = 1;

sub getProperty {
    my ($ec, $name, $mandatory, $default) = @_;
    my $ret = $ec->getProperty($name)->findvalue('//value')->string_value;

    if(!$ret && $mandatory) {
        die "Missing mandatory parameter '$name'.";
    }

    return $ret || $default;
}

# get an EC object
my $ec = ElectricCommander->new();

my $docker_compose_file_content = getProperty($ec, "docker_compose_file_content", 0);
my $project = getProperty($ec, "project", 1);
my $application = getProperty($ec, "application", 0);


print "Project: $project\n";
print "Application: $application\n";

my $xpath;
my $exists = 0;
eval {
    $xpath = $ec->getApplication({projectName => $project, applicationName => $application});
    $exists = 1;
    1;
};
if ($exists) {
    print "Application $application already exists\n";
}
else {
    $xpath = $ec->createApplication({applicationName => $application, projectName => $project});
    my $application_id = $xpath->findvalue('//applicationId')->string_value;
    print "Application has been created: id $application_id\n";
}