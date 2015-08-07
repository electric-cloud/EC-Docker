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
my $detached_mode = getProperty($ec, "detached_mode", 0);
my $yaml_file = getProperty($ec, "yaml_file", 0);

print "Detached mode: $detached_mode\n\n";

my $command;
if($use_sudo) {
	$command = "sudo docker-compose up";
} else {
	$command = "docker-compose up";
}

if($detached_mode) {
	$command .= " -d 2>&1";
}

if($yaml_file) {
	open (YAML, ">docker-compose.yml") or die "Can't open docker-compose.yml file for writing.";
	print YAML $yaml_file;
    close (YAML);
	print "Updated docker-compose.yml file.\n\n";
}

print "Command to execute: $command\n";
print "Running the command:\n\n";

my $docker_output = system($command);
if($? != 0) {
    $ec->setProperty("summary", "exit code $?");
    $ec->setProperty("outcome", "error");
}
print $docker_output . "\n";
