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
my $build_path = getProperty($ec, "build_path", 1);

if(!defined $build_path) {
    die "ERROR: build path parameter is required but not set.";
}

print "Build path: $build_path\n";

my $command;
if($use_sudo) {
	$command = "sudo docker build $build_path 2>&1";
} else {
	$command = "docker build $build_path 2>&1";
}

print "Command to execute: $command\n";
print "Building docker image:\n\n";

my $docker_output = system($command);
if($? != 0) {
    $ec->setProperty("summary", "exit code $?");
    $ec->setProperty("outcome", "error");
}
print $docker_output . "\n";
