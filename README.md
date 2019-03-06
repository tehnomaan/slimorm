# SlimORM
Lightweight Java ORM library

# Building
Install GnuPG from https://www.gnupg.org/download/ .
In environment, set GRADLE_USER_HOME=C:/directory-for-gradle.properties

File gradle.properties should look like this:
```
signing.keyId=ECCAD9C3
signing.password=kala1234
signing.secretKeyRingFile=C:/path/to/secring.gpg
signing.gnupg.executable=gpg
signing.gnupg.useLegacyGpg=true
signing.gnupg.keyName=ECCAD9C3
signing.gnupg.passphrase=xyz

ossrhUsername=your-jira-id
ossrhPassword=your-jira-password
```

Start building
```
gradle build
```
