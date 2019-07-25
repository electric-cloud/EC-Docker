# use ElectricCommander;
# my $commander = ElectricCommander->new;
# my $pluginName = 'EC-Docker-1.5.0.0';
# my $promoteAction = 'promote';
# my $upgradeAction = '';
# my $otherPluginName = '';


# Plugin-specific setup code
my $setup = ECSetup->new(
    commander => $commander,
    pluginName => $pluginName,
    otherPluginName => $otherPluginName,
    upgradeAction => $upgradeAction,
    promoteAction => $promoteAction,
);
$setup->promotePlugin([
    {artifactName => '@PLUGIN_KEY@-Grapes', artifactVersion => '1.0.3', fromDirectory => 'lib'}
]);


# ec_setup.pl shared code
#####################################
package ECSetup;

use strict;
use warnings;

use File::Spec;
use Archive::Zip;
use MIME::Base64;
use Digest::MD5 qw(md5_hex);
use File::Temp qw(tempfile tempdir);
use Cwd;
use POSIX;
use JSON;
use Data::Dumper;


sub new {
    my ($class, %param) = @_;

    my $self = { %param };
    $self->{commander} or die '$commander object should be provided!';
    $self->{promoteAction} or die '$promoteAction should be provided!';
    defined $self->{upgradeAction} or die '$upgradeAction should be provided!';
    $self->{pluginName} or die '$pluginName should be provided!';
    defined $self->{otherPluginName} or die '$otherPluginName should be provided!';

    return bless $self, $class;
}

sub commander { shift->{commander} }
sub promoteAction { shift->{promoteAction} }
sub upgradeAction { shift->{upgradeAction} }
sub pluginName { shift->{pluginName} }
sub otherPluginName { shift->{otherPluginName} }

sub publishArtifact {
    my ($self, $artifactName, $artifactVersion, $fromDirectory) = @_;

    $artifactName or die 'Artifact name should be provided!';
    $artifactVersion or die 'Artifact version should be provided!';
    $fromDirectory or die 'fromDirectory should be provided!';

    # This is here because we cannot do publishArtifactVersion in dsl today
    # delete artifact if it exists first
    my $commander = $self->commander;
    $commander->deleteArtifactVersion("com.electriccloud:$artifactName:$artifactVersion");


    my ($tempFh, $tempFilename) = tempfile(CLEANUP => 1);
    my $dsl = q{
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream


int length = args.length
int chunkNumber = args.chunkNumber

// take this from the server
def pluginsFolder = getProperty(propertyName: '/server/settings/pluginsDirectory')?.value
File root = new File(pluginsFolder.toString(), '@PLUGIN_NAME@')
File zipArchive = new File(root, 'dependencies.zip')
if (!zipArchive.exists()) {
    ZipOutputStream zipFile = new ZipOutputStream(new FileOutputStream(zipArchive))
    new File(root, 'lib').eachFileRecurse { file ->
        //check if file
        if (file.isFile()) {
            def relPath = root.toPath().relativize(file.toPath()).toString()
            zipFile.putNextEntry(new ZipEntry(relPath))
            def buffer = new byte[file.size()]
            file.withInputStream {
                zipFile.write(buffer, 0, it.read(buffer))
            }
            zipFile.closeEntry()
        }
    }
    zipFile.close()
}

FileInputStream is = new FileInputStream(zipArchive);
byte[] chunk = new byte[length];
int bytesRead = 0
int counter = 0
def response
while(bytesRead != -1 && counter <= chunkNumber) {
    bytesRead = is.read(chunk)
    response = [
        hasMore: (bytesRead != -1),
        read: bytesRead,
    ]
    if (bytesRead > 0) {
        chunk = chunk[0 .. bytesRead-1]
        response.chunk = chunk.encodeBase64().toString()
    }
    counter ++
}

return response

    };


    my $length = 1024 * 1024 * 5;
    my $chunkNumber = 0;

    my $hasMore = 1;
    while($hasMore) {
        my $result = $commander->evalDsl($dsl,
            {
                parameters => qq(
                    {
                        "length": $length,
                        "chunkNumber": $chunkNumber
                    }
                )
            }
        );
        my $json = $result->findvalue('//value')->string_value;
        my $object = decode_json($json);
        my $chunk = $object->{chunk};
        if ($chunk) {
            my $decoded = decode_base64($chunk);
            print $tempFh $decoded;
            warn "Got chunk\n";
        }
        $hasMore = $object->{hasMore};
        $chunkNumber ++;
        delete $object->{chunk};
        warn Dumper $object;
    }
    close $tempFh;

    my ($tempDir) = tempdir(CLEANUP => 1);
    my $zip = Archive::Zip->new();
    unless($zip->read($tempFilename) == Archive::Zip::AZ_OK()) {
      die "Cannot read .zip dependencies from $tempFilename: $!";
    }
    $zip->extractTree("", $tempDir . '/');

    my $logfile = '';
    if ( $self->promoteAction eq "promote" ) {
        #publish jars to the repo server if the plugin project was created successfully
        my $am = new ElectricCommander::ArtifactManagement($commander);
        my $artifactVersion = $am->publish(
            {   groupId         => "com.electriccloud",
                artifactKey     => "$artifactName",
                version         => $artifactVersion,
                includePatterns => "**",
                fromDirectory   => File::Spec->catfile($tempDir, $fromDirectory),
                description => 'JARs that @PLUGIN_NAME@ plugin procedures depend on'
            }
        );

        # Print out the xml of the published artifactVersion.
        $logfile .= $artifactVersion->xml() . "\n";

        if ( $artifactVersion->diagnostics() ) {
            $logfile .= "\nDetails:\n" . $artifactVersion->diagnostics();
        }
    }

    return $logfile;
}

sub promotePlugin {
    my ($self, $dependencies) = @_;

    my $logfile = "";

    my $commander = $self->commander;
    my $pluginName = $self->pluginName;

    if ($dependencies) {
        for my $dependency (@$dependencies) {
            $logfile .= $self->publishArtifact($dependency->{artifactName}, $dependency->{artifactVersion}, $dependency->{fromDirectory});
        }
    }

    # Create output property for plugin setup debug logs
    my $nowString = localtime;
    $commander->setProperty( "/plugins/$pluginName/project/logs/$nowString", { value => $logfile } );
}
