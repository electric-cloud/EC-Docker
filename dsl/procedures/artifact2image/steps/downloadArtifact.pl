use strict;
use warnings;
use Data::Dumper;
use ElectricCommander;
use ElectricCommander::ArtifactManagement;
use ElectricCommander::ArtifactManagement::ArtifactVersion;
use Cwd qw(getcwd);
use File::Spec;



my $ec = ElectricCommander->new();

my $artifactName = getParam('artifactName');
my $location = getParam('artifactLocation');
my $artifactoryConfig = getParam('artifactoryConfigName');

if ($artifactName) {
    my $versionRange = getParam('versionRange');
    retrieveArtifactFromRepository($artifactName, $versionRange);
}
elsif ($location) {
#    do nothing
}
elsif ($artifactoryConfig) {
    retrieveArtifactFromArtifactory($artifactoryConfig);
}
else {
    print "Either artifact name, Artifactory details or artifact location must be specified\n";
    exit -1;
}


sub handleError {
    my ($message) = @_;

    print $message . "\n";
    exit -1;
}

sub retrieveArtifactFromArtifactory {
    my ($configName) = @_;


    my $parameters = {
        artifact   => 'artifactoryArtifactName',
        config     => 'artifactoryConfigName',
        repoType   => 'artifactoryRepoType',
        orgPath    => 'artifactoryOrgPath',
        extension  => 'artifactoryExtension',
        repository => 'artifactoryRepoKey',
    };

    my $actualParameters = [];
    for my $artifactoryParamName (keys %$parameters) {
        my $value = getParam($parameters->{$artifactoryParamName});
        if (!$value && $artifactoryParamName != 'version') {
            print qq{Parameter "$parameters->{$artifactoryParamName}" is required\n};
            exit -1;
        }
        push @$actualParameters, {actualParameterName => $artifactoryParamName, value => $value};
    }

    my $artifactName = getParam('artifactoryArtifactName');
    my $destination = File::Spec->catfile(getcwd(), $artifactName . '-dockerfile');


    push @$actualParameters, {
        actualParameterName => 'useRepositoryLayout',
        value => '1',
    };
    push @$actualParameters, {
        actualParameterName => 'destination',
        value => $destination,
    };

    my $version = getParam('artifactoryVersion');
    if ($version) {
        push @$actualParameters, {
            actualParameterName => 'version',
            value => $version,
        };
    }
    else {
        push @$actualParameters, {
            actualParameterName => 'latestVersion',
            value               => '1'
        };
    }

    print Dumper $actualParameters;


    my $xpath = $ec->createJobStep({
        subprocedure    => 'Retrieve Artifact from Artifactory',
        subproject      => '/plugins/EC-Artifactory/project',
        actualParameter => $actualParameters,
    });

    print $xpath->{_xml};

    $ec->setProperty("/myJob/$artifactName/location", $destination);
}



sub retrieveArtifactFromRepository {
    my ($artifactName, $versionRange) = @_;

    my $am = new ElectricCommander::ArtifactManagement($ec);

    print "Artifact Name: $artifactName\n";
    print "Version Range: $versionRange\n";

    my $destination = File::Spec->catfile(getcwd(), $artifactName . '-dockerfile');

    my ($group, $name, $version) = split(':', $artifactName);

    my @retrievedArtifactVersions = $am->retrieve({
        groupId      => $group,
        artifactKey  => $name,
        versionRange => $versionRange,
        toDirectory  => $destination,
    });

    if (scalar @retrievedArtifactVersions != 1) {
        if (scalar @retrievedArtifactVersions == 0) {
            print "No artifact versions found!\n";
            exit -1;
        }
        else {
            print "More than one version found!\n";
            for my $v (@retrievedArtifactVersions) {
                print $v->diagnostics() . "\n";
            }
            exit -1;
        }
    }

    my $retrievedVersion = $retrievedArtifactVersions[0];
    print "Retrieval result: @{[ $retrievedVersion->diagnostics() ]}\n";
    $ec->setProperty("/myJob/$artifactName/location", $destination);
    $ec->setProperty("/myJobStep/summary", "Retrieved @{[ $retrievedVersion->artifactVersionName ]}");

}

sub getParam {
    my ($paramName) = @_;

    my $xpath = $ec->getProperty("ecp_docker_$paramName");
    return $xpath->findvalue('//value')->string_value;
}