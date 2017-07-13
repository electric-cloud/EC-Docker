@certFiles = ("$ENV{'HOME'}/.docker/cert/ca.pem","$ENV{'HOME'}/.docker/cert/key.pem","$ENV{'HOME'}/.docker/cert/cert.pem");
chmod 0600, @certFiles;