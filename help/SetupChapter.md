Dependency Management (Setup procedure)
========================
Binary dependencies (Grapes) are loaded from ElectricFlow server through DSL.
Upon procedure run a cache descriptor under plugin project (ec_dependenciesCache property) is checked.
If the property does not exist, or the files listed there are missing on disk, or .jar files are corrupt, the depedencies will be downloaded from ElectricFlow server, and this can take some time.
Subsequent runs will be much faster as they will be using already downloaded and cached files.
