use strict;
use warnings;
use Data::Dumper;
use ElectricCommander;
use ElectricCommander::ArtifactManagement;
use ElectricCommander::ArtifactManagement::ArtifactVersion;
use Cwd qw(getcwd);
use File::Spec;

my $ec = ElectricCommander->new();
my $am = new ElectricCommander::ArtifactManagement($ec);

my $artifactName = $ec->getProperty('ec_docker_artifactName')->findvalue('//value')->string_value;

my ($group, $name, $version) = split(':', $artifactName);
my $destination = File::Spec->catfile(getcwd(), $artifactName . '-dockerfile');

my @retrievedArtifactVersions = $am->retrieve({
    groupId => $group,
    artifactKey  => $name,
    versionRange => $version,
    toDirectory => $destination,
});

print Dumper \@retrievedArtifactVersions;

my $version = $retrievedArtifactVersions[0];
print "Retrieval result: @{[$version->diagnostics()]}\n";
$ec->setProperty("/myJob/$artifactName/location", $destination);