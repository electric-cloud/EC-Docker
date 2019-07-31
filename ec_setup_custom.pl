use strict;
use ElectricCommander;

# my $commander = ElectricCommander->new;

my $propertySheetName = 'ec_scratch';
my $xpath = $commander->createProperty({
    propertyType => 'sheet',
    propertyName => $propertySheetName,
    projectName => "/projects/$pluginName",
});

print "Plugin name: $pluginName\n";

my $propertySheetId = $xpath->findvalue('//propertySheetId')->string_value;

print "Created property sheet $propertySheetId\n";

$commander->createAclEntry({
    principalType => 'group',
    principalName => 'Everyone',
    propertySheetId => $propertySheetId,
    objectType => 'propertySheet',
    readPrivilege => 'allow',
    modifyPrivilege => 'allow',
    executePrivilege => 'allow',
});

print "Created ACL for scratchpad\n";
