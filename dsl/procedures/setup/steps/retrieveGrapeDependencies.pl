# Version: Tue Nov  6 19:00:08 2018
#
#  Copyright 2016 Electric Cloud, Inc.
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

=head1 NAME

retrieveGrapeDependencies.pl

=head1 DESCRIPTION


Retrieves artifacts published as artifact EC-Docker-Grapes
to the grape root directory configured with ec-groovy.

=head1 METHODS

=cut

use File::Copy::Recursive qw(rcopy);
use File::Path;
use ElectricCommander;

use warnings;
use strict;
$|=1;


$::gAdditionalArtifactVersion = "$[additionalArtifactVersion]";

sub main() {
    my $ec = ElectricCommander->new();
    $ec->abortOnError(1);

    my $pluginName = eval {
        $ec->getProperty('additionalPluginName')->findvalue('//value')->string_value
    };
    my @projects = ();
    push @projects, '$[/myProject/projectName]';

    if ($pluginName) {
        # This is a new one
        my @names = split(/\s*,\s*/, $pluginName);
        for my $name (@names) {
            my $projectName = $ec->getPlugin($pluginName)->findvalue('//projectName')->string_value;
            push @projects, $projectName;
        }
    }

    my $dep = EC::DependencyManager->new($ec);
    $dep->grabResource();
    for my $project (@projects) {
        eval {
            $dep->transferWithDsl($project);
            1;
        } or do {
            $ec->setProperty('/myJobStep/summary', "Failed to download dependencies: $@");
            exit 1;
        };
    }


    # This part remains as is
    if ($::gAdditionalArtifactVersion ne '') {
        my @versions = split(/\s*,\s*/, $::gAdditionalArtifactVersion);
        for my $version (@versions) {
            retrieveGrapeDependency($ec, $version);
        }
    }
}



########################################################################
# retrieveGrapeDependency - Retrieves the artifact version and copies it
# to the grape directory used by ec-groovy for @Grab dependencies
#
# Arguments:
#   -ec
#   -artifactVersion
########################################################################
sub retrieveGrapeDependency($){
    my ($ec, $artifactVersion) = @_;

    my $xpath = $ec->retrieveArtifactVersions({
        artifactVersionName => $artifactVersion
    });

    # copy to the grape directory ourselves instead of letting
    # retrieveArtifactVersions download to it directly to give
    # us better control over the over-write/update capability.
    # We want to copy only files the retrieved files leaving
    # the other files in the grapes directory unchanged.
    my $dataDir = $ENV{COMMANDER_DATA};
    die "ERROR: Data directory not defined!" unless ($dataDir);

    my $grapesDir = $ENV{COMMANDER_DATA} . '/grape/grapes';
    my $dir = $xpath->findvalue("//artifactVersion/cacheDirectory");

    mkpath($grapesDir);
    die "ERROR: Cannot create target directory" unless( -e $grapesDir );

    rcopy( $dir, $grapesDir) or die "Copy failed: $!";
    print "Retrieved and copied grape dependencies from $dir to $grapesDir\n";
}

main();

1;


package EC::DependencyManager;
use strict;
use warnings;

use File::Spec;
use JSON qw(decode_json encode_json);
use File::Copy::Recursive qw(rcopy rmove);
use File::Find;
use Digest::MD5 qw(md5_hex);
use Data::Dumper;
use subs qw(info debug);
use MIME::Base64 qw(decode_base64);
use File::Basename qw(dirname);
use File::Path qw(mkpath);

sub new {
    my ($class, $ec, %options) = @_;

    my $self = { ec => $ec };
    if (!$ENV{COMMANDER_DATA}) {
        die "Environment variable COMMANDER_DATA must be set";
    }
    $self->{dest} = $options{destination} || "$ENV{COMMANDER_DATA}/grape";
    return bless $self, $class;
}


sub ec {
    return shift->{ec};
}

sub destination {
    return shift->{dest};
}

sub grabResource {
    my ($self) = @_;

    my $resName = '$[/myResource/resourceName]';
    $self->ec->setProperty('/myJob/grabbedResource', $resName);
    info "Grabbed Resource: $resName";
}



sub getPluginsFolder {
    my ($self) = @_;

    return $self->ec->getProperty('/server/settings/pluginsDirectory')->findvalue('//value')->string_value;
}


sub printLog {
    my ($marker, @messages) = @_;

    for my $message (@messages) {
        if (ref $message) {
            $message = Dumper($message);
        }
        print "$marker $message\n";
    }
}


sub debug {
    my @messages = @_;

    if ($ENV{DEPENDENCIES_DEBUG}) {
        printLog('[DEBUG]', @messages);
    }
}


sub info {
    my @messages = @_;

    printLog('[INFO]', @messages);
}


sub checkChecksums {
    my ($self, $project) = @_;

    my $deps = eval {
        my $string = $self->ec->getProperty("/projects/$project/ec_dependenciesCache")->findvalue('//value')->string_value;
        decode_json($string);
    };

    return 0 unless $deps;
    return 0 unless $deps->{files};
    return 0 unless $deps->{checksum};

    my $checksum = $self->calculateCacheChecksum($deps->{files});
    if ($checksum ne $deps->{checksum}) {
        info "Checksums do not match: $deps->{checksum} and $checksum\n";
        return 0;
    }
    return 1;
}


sub calculateCacheChecksum {
    my ($self, $files) = @_;

    my $digest = Digest::MD5->new;
    for my $file (@{$files}) {
        my $filename = File::Spec->catfile($self->destination, $file);
        open my $fh, $filename or return 0;
        binmode $fh;
        next unless $filename =~ /jar$/; # descriptors tend to change slightly upon ec-groovy runs - xml reformatting, dates etc.

        my $content = join('', <$fh>);
        close $fh;
        $digest->add($content);
    }

    my $checksum = $digest->hexdigest;
    return $checksum;
}

sub setSummary {
    my ($self, $summary) = @_;

    $self->ec->setProperty('/myJobStep/summary', $summary);
}

sub acquireLock {
    my ($self, $projectName, $resName) = @_;

    my $time;
    my $property = "/projects/$projectName/ec_dependenciesLock$resName";
    eval {
        $time = $self->ec->getProperty($property)->findvalue('//value')->string_value;
        1;
    } or do {
        $self->ec->setProperty($property, time);
    };

    if ($time) {
        my $duration = 60;
        if ($time+$duration > time) {
            return 0;
        }
        else {
            info "Staled lock found in project $projectName";
            $self->ec->setProperty($property, time);
            return 1;
        }
    }
    else {
        return 1;
    }
}


sub releaseLock {
    my ($self, $projectName, $resName) = @_;

    my $property = "/projects/$projectName/ec_dependenciesLock$resName";
    eval {
        $self->ec->deleteProperty($property);
        1;
    } or do {
        info "Failed to release lock: $@";
    };
}

sub transferWithDsl {
    my ($self, $projectName) = @_;

    my $resourceName = '$[/myResource/resourceName]';
    info "Processing dependencies for plugin $projectName";
    my $lock = $self->acquireLock($projectName, $resourceName);
    if (!$lock) {
        print "[INFO] Someone else is downloading dependencies for the project $projectName ($resourceName), waiting...";
    }
    my $newline = 0;
    while (!$lock) {
        print '.';
        sleep 2;
        $lock = $self->acquireLock($projectName, $resourceName);
        $newline = 1;
    }
    print "\n" if $newline;

    my $checksumsOk = 1;
    unless($self->checkChecksums($projectName)) {
        $checksumsOk = 0;
    }

    if ($checksumsOk) {
        info "Dependencies cache is ok, no dependency transfer is required";
        $self->setSummary("Dependencies will be taken from the local cache");
        $self->releaseLock($projectName, $resourceName);
        return 0;
    }

    my $dsl = q{
import java.util.zip.*
import java.security.MessageDigest
import groovy.json.JsonOutput

def path = args.path
def chunkSize = args.chunkSize ?: 1024 * 1024
def number = args.counter ?: 0
File lib = new File(path)

def files = []
lib.eachFileRecurse { f ->
    if (f.isFile()) {
        files << f
    }
}

def encodedFiles = [:]
def fileList = []
files.sort { a, b -> a.absolutePath <=> b.absolutePath }.each { f ->
    def relPath = lib.toURI().relativize(f.toURI()).toString()
    fileList << relPath
    def base64 = f.bytes.encodeBase64().toString()
    encodedFiles[relPath] = base64
}

def result = JsonOutput.toJson(encodedFiles).getBytes().encodeBase64().toString()
def length = result.length()

def start = number * chunkSize
def end = (number + 1) * chunkSize - 1
def hasMore = true
if (end >= length) {
    end = length - 1
    hasMore = false
}

def chunk = result[start .. end]
return [
  chunk: chunk,
  chunkLength: chunk.length(),
  length: length,
  hasMore: hasMore,
  checksum: generateMD5(result.bytes),
  files: fileList
]

// Older version of Groovy does not have String.md5()
def generateMD5(byte[] bytes) {
   MessageDigest digest = MessageDigest.getInstance("MD5")
   digest.update(bytes)
   byte[] md5sum = digest.digest()
   BigInteger bigInt = new BigInteger(1, md5sum)
   return bigInt.toString(16).padLeft(32, '0')
}
};

    my $libPath = $self->ec->getProperty('/server/settings/pluginsDirectory')->findvalue('//value')->string_value . "/$projectName/lib";
    my $chunkSize = 1024 * 1024;
    my $hasMore = 1;
    my $base64 = '';
    my $counter = 0;
    my $checksum = '';

    my $fileList;
    print "[INFO] Downloading dependencies from server";
    while($hasMore) {
        my $parameters = encode_json({
            chunkSize => $chunkSize,
            path => $libPath,
            counter => $counter + 0,
        });
        my $xpath = $self->ec->evalDsl({
            dsl => $dsl,
            parameters => $parameters,
            debug => 'false'
        });
        my $json = $xpath->findvalue('//value')->string_value;
        my $data = decode_json($json);
        $hasMore = $data->{hasMore};
        $base64 .= $data->{chunk};

        $counter ++;
        $checksum = $data->{checksum};
        my $printable = $data;
        delete $printable->{chunk};
        debug Dumper $printable;
        $fileList = $data->{files};
        print '.';
    }
    print "\n";

    my $resultChecksum = md5_hex($base64);
    if ($resultChecksum ne $checksum) {
        die "Wrong checksum: $checksum ne $resultChecksum";
    }

    my $files = decode_json(decode_base64($base64));
    my $grape = $ENV{COMMANDER_DATA} . "/grape";

    for my $file (keys %$files) {
        my $destination = File::Spec->catfile($grape, $file);
        my $dir = dirname($destination);
        unless( -e $dir) {
            mkpath($dir);
        }
        open my $fh, ">$destination" or die "Cannot open $destination: $!";
        binmode $fh;
        my $binary = decode_base64($files->{$file});
        print $fh $binary;
        close $fh;
        debug "Saved file $destination";
    }

    my $cacheChecksum = $self->calculateCacheChecksum($fileList);

    $self->ec->setProperty("/projects/$projectName/ec_dependenciesCache", encode_json({checksum => $cacheChecksum, files => $fileList}));
    info "Saved dependencies into cache";
    $self->setSummary("Dependencies are downloaded and saved into local cache for project $projectName");
    $self->releaseLock($projectName, $resourceName);
}

1;
