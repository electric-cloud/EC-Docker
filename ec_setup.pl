use Cwd;
use File::Spec;
use POSIX;
use Archive::Zip;
use MIME::Base64;
use File::Temp qw(tempfile tempdir);
my $dir = getcwd;
my $logfile ="";
my $pluginDir;


if ( defined $ENV{QUERY_STRING} ) {    # Promotion through UI
    $pluginDir = $ENV{COMMANDER_PLUGINS} . "/$pluginName";
}
else {
    my $commanderPluginDir = $commander->getProperty('/server/settings/pluginsDirectory')->findvalue('//value');
    $pluginDir = File::Spec->catfile($commanderPluginDir, $pluginName);
}

$logfile .= "Plugin directory is $pluginDir\n";

$commander->setProperty("/plugins/$pluginName/project/pluginDir", {value=>$pluginDir});
$logfile .= "Plugin Name: $pluginName\n";
$logfile .= "Current directory: $dir\n";

# Evaluate promote.groovy or demote.groovy based on whether plugin is being promoted or demoted ($promoteAction)
local $/ = undef;

my $demoteDsl = q{
# demote.groovy placeholder
};

my $promoteDsl = q{
# promote.groovy placeholder
};


my $dsl;
if ($promoteAction eq 'promote') {
  $dsl = $promoteDsl;
}
else {
  $dsl = $demoteDsl;
}


my $dslReponse = $commander->evalDsl(
    $dsl, {
        parameters => qq(
                     {
                       "pluginName":"$pluginName",
                       "upgradeAction":"$upgradeAction",
                       "otherPluginName":"$otherPluginName"
                     }
              ),
        debug             => 'false',
        serverLibraryPath => File::Spec->catdir( $pluginDir, 'dsl' ),
    },
);


$logfile .= $dslReponse->findnodes_as_string("/");

my $errorMessage = $commander->getError();
if ( !$errorMessage ) {

    # This is here because we cannot do publishArtifactVersion in dsl today

    # delete artifact if it exists first
    $commander->deleteArtifactVersion("com.electriccloud:EC-Docker-Grapes:1.0.1");

    my $dependenciesProperty = '/projects/@PLUGIN_NAME@/ec_groovyDependencies';
    my $base64 = $commander->getProperty($dependenciesProperty)->findvalue('//value')->string_value;
    my $binary = decode_base64($base64);
    my ($tempFh, $tempFilename) = tempfile(CLEANUP => 1);
    binmode($tempFh);
    print $tempFh $binary;
    close $tempFh;

    my ($tempDir) = tempdir(CLEANUP => 1);
    my $zip = Archive::Zip->new();
    unless($zip->read($tempFilename) == Archive::Zip::AZ_OK()) {
      die "Cannot read .zip dependencies: $!";
    }
    $zip->extractTree("", File::Spec->catfile($tempDir, ''));


    if ( $promoteAction eq "promote" ) {

        #publish jars to the repo server if the plugin project was created successfully
        my $am = new ElectricCommander::ArtifactManagement($commander);
        my $artifactVersion = $am->publish(
            {   groupId         => "com.electriccloud",
                artifactKey     => "EC-Docker-Grapes",
                version         => "1.0.1",
                includePatterns => "**",
                fromDirectory   => "$tempDir/lib",
                description => "JARs that EC-Docker plugin procedures depend on"
            }
        );

        # Print out the xml of the published artifactVersion.
        $logfile .= $artifactVersion->xml() . "\n";

        if ( $artifactVersion->diagnostics() ) {
            $logfile .= "\nDetails:\n" . $artifactVersion->diagnostics();
        }
    }
}
# Create output property for plugin setup debug logs
my $nowString = localtime;
$commander->setProperty( "/plugins/$pluginName/project/logs/$nowString", { value => $logfile } );

die $errorMessage unless !$errorMessage
