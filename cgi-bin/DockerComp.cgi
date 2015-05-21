#!/bin/sh

exec "$COMMANDER_HOME/bin/ec-perl" -x "$0" "${@}"

#!perl

use strict;
use ElectricCommander;
use CGI;

$| = 1;

# Create a single instance of the Perl access to ElectricCommander
my $ec = new ElectricCommander();

print "Content-type: text/html\n\n";

print "Hello World!";
