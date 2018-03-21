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
my $versionRange = $ec->getProperty('ecp_docker_versionRange')->findvalue('//value')->string_value;

print "Artifact Name: $artifactName\n";
print "Version Range: $versionRange\n";

my ($group, $name, $version) = split(':', $artifactName);
my $destination = File::Spec->catfile(getcwd(), $artifactName . '-dockerfile');

my @retrievedArtifactVersions = $am->retrieve({
    groupId => $group,
    artifactKey  => $name,
    versionRange => $versionRange,
    toDirectory => $destination,
});

if (scalar @$retrievedArtifactVersions != 1) {
    if (scalar @$retrievedArtifactVersions == 0) {
        print "No artifact versions found!\n";
        exit -1;
    }
    else {
        print "More than one version found!\n";
        for my $v (@$retrievedArtifactVersions) {
            print $v->diagnostics() . "\n";
        }
        exit -1;
    }
}

my $retrievedVersion = $retrievedArtifactVersions[0];
print "Retrieval result: @{[$retrievedVersion->diagnostics()]}\n";
$ec->setProperty("/myJob/$artifactName/location", $destination);
$ec->setProperty("/myJobStep/summary", "Retrieved @{[$retrievedVersion->artifactVersionName]}");
