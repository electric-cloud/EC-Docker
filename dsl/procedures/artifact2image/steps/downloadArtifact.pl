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

my $artifactName = $ec->getProperty('ecp_docker_artifactName')->findvalue('//value')->string_value;

my ($group, $name, $version) = split(':', $artifactName);
my $destination = File::Spec->catfile(getcwd(), $artifactName . '-dockerfile');

my @retrievedArtifactVersions = $am->retrieve({
    groupId => $group,
    artifactKey  => $name,
    versionRange => $version,
    toDirectory => $destination,
});

my $retrievedVersion = $retrievedArtifactVersions[0];
print "Retrieval result: @{[$retrievedVersion->diagnostics()]}\n";
$ec->setProperty("/myJob/$artifactName/location", $destination);
$ec->setProperty("/myJobStep/summary", "Retrieved @{[$retrievedVersion->artifactVersionName]}");
