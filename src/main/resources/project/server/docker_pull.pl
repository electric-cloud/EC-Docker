use ElectricCommander;

$| = 1;

sub getProperty {
    my ($ec, $name, $mandatory, $default) = @_;
    $ret = $ec->getProperty($name)->findvalue('//value')->string_value;
    
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
my $all_tags = getProperty($ec, "all_tags", 0);

if(!defined $image_name) {
    die "ERROR: image name parameter is required but not set.";
}

print "Image Name: $image_name\n";
print "All tags:   $all_tags\n";

my $command;
if($use_sudo) {
	$command = "sudo docker pull";
} else {
	$command = "docker pull";
}

if($all_tags) {
    $command .= " -a";
}

$command .= " $image_name 2>&1";

print "\nCommand to execute: $command\n";
print "Pulling docker image:\n\n";

my $out = `$command`;
print $out . "\n";