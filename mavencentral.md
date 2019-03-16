# SlimORM - Deploying to Maven Central

* Install GnuPG from https://www.gnupg.org/download/
* In environment, set GRADLE\_USER\_HOME=C:/directory-for-gradle-properties
* File gradle.properties (in GRADLE\_USER\_HOME) should look like this:

```
nexusUsername=ossrh-username
nexusPassword=ossrh-password
signing.keyId=ECCAD9C3
signing.password=gpg-password
signing.secretKeyRingFile=/path/to/secring.gpg
```

* Uploading steps

```
gradle clean install
gradle upload
gradle closeAndReleaseRepository
```

Alternatively, if the last command fails, closing and releasing can be done manually via [Nexus Repository Manager](https://oss.sonatype.org/#stagingRepositories).

