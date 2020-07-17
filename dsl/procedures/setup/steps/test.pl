# Version: Tue Nov  6 19:00:08 2018
use strict;
use warnings;
use ElectricCommander;
use Data::Dumper;
use Digest::MD5;
use JSON qw(decode_json);

my $ec = ElectricCommander->new;
my $project = "EC-Kubernetes-1.2.0";
print checkChecksums($project);

sub checkChecksums {
    my ( $project) = @_;

    my $deps = eval {
        my $string = $ec->getProperty("/projects/$project/ec_dependencies")->findvalue('//value')->string_value;
        decode_json($string);
    };

    return 0 unless $deps;
    return 0 unless $deps->{files};
    return 0 unless $deps->{checksum};
    my $digest = Digest::MD5->new;
    print "Files: " . scalar @{$deps->{files}} . "\n";

    for my $file (@{$deps->{files}}) {
        my $filename = File::Spec->catfile($ENV{COMMANDER_DATA}, 'grape', $file);
        open my $fh, $filename or return 0;
        binmode $fh;
        $digest->addfile($fh);
    }

    my $checksum = $digest->hexdigest;
    if ($checksum ne $deps->{checksum}) {
        print "Checksums do not match: $deps->{checksum} and $checksum\n";
        return 0;
    }
    return 1;
}
