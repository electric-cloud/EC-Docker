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
$ec->abortOnError(0);

my $use_sudo = getProperty($ec, "use_sudo", 0);
my $image_name = getProperty($ec, "image_name", 1);
my $image_tag = getProperty($ec, "tag", 0);

if(!defined $image_name) {
    die "ERROR: image name parameter is required but not set.";
}

print "Image Name: $image_name\n";
print "Image tag:  $image_tag\n";

my $command;
if($use_sudo) {
	$command = "sudo docker pull";
} else {
	$command = "docker pull";
}

if($image_tag) {
    $image_name .= ":$image_tag";
}

$command .= " $image_name 2>&1";

print "\nCommand to execute: $command\n";
print "Pulling docker image:\n\n";

my $docker_output = system($command);
if($? != 0) {
    $ec->setProperty("summary", "exit code $?");
    $ec->setProperty("outcome", "error");
}
print $docker_output . "\n";
