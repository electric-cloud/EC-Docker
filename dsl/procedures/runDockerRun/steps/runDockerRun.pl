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
my $container_name = getProperty($ec, "container_name", 0);
my $detached_mode = getProperty($ec, "detached_mode", 0);
my $entrypoint = getProperty($ec, "entrypoint", 0);
my $working_dir = getProperty($ec, "working_dir", 0);
my $published_ports = getProperty($ec, "published_ports", 0);
my $publish_all_ports = getProperty($ec, "publish_all_ports", 0);
my $privileged = getProperty($ec, "privileged", 0);
my $container_links = getProperty($ec, "container_links", 0);
my $command_with_args = getProperty($ec, "command_with_args", 0);

if(!defined $image_name) {
    die "ERROR: image name parameter is required, but not set.";
}

print "Image Name:        $image_name\n";
print "Container Name:    $container_name\n";
print "Detached mode:     $detached_mode\n";
print "Entrypoint:        $entrypoint\n";
print "Working dir:       $working_dir\n";
print "Published ports:   $published_ports\n";
print "Publish all ports: $publish_all_ports\n";
print "Privileged:        $privileged\n";
print "Container links:   $container_links\n";
print "Command with args: $command_with_args\n";

my $command;
if($use_sudo) {
	$command = "sudo docker run";
} else {
	$command = "docker run";
}

if($container_name) {
	$command .= " --name=\"$container_name\"";
}

if($detached_mode) {
	$command .= " -d";
}

if($entrypoint) {
	$command .= " --entrypoint=\"$entrypoint\"";
}

if($working_dir) {
	$command .= " --workdir=\"$working_dir\"";
}

if($published_ports) {
	my @port_mappings = split / /, $published_ports;
	foreach my $mapping (@port_mappings) {
		$command .= " -p $mapping";
	}
}

if($publish_all_ports) {
	$command .= " -P";
}

if($container_links) {
	$command .= " --link $container_links";
}

if($privileged) {
	$command .= " --privileged=true";
}

$command .= " $image_name";

if($command_with_args) {
	$command .= " $command_with_args";
}

$command .= " 2>&1";

print "\nCommand to execute: $command\n";
print "Launching docker container:\n\n";

my $docker_output = system($command);
if($? != 0) {
    $ec->setProperty("summary", "exit code $?");
    $ec->setProperty("outcome", "error");
}
print $docker_output . "\n";
