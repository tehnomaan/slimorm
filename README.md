# SlimORM
SlimORM is a lightweight Java ORM library.

Why yet another ORM library, when we have popular ORM libraries already?
Because sometimes, You just don't want the complexity of those popular libraries.
Instead, You need something simple (simple to set up, simple to use, minimal dependencies).
For large and complex enterprise projects, SlimORM probably lacks features and flexibility.

SlimORM uses  

# Usage

```java
Database db = new Database("org.postgresql.Driver", "jdbc:postgresql://localhost:5432/demoDB", "demouser", "password");

Employee employee = new Employee();
employee.name = "John Smith";
employee.dateOfBirth = LocalDate.of(1992, 3, 27);
db.insert(employee);

employee.dateOfBirth = LocalDate.of(1993, 5, 14);
db.update(employee);

db.delete(Employee.class, employee.id);
```

# Annotations

* @Table
* @Column
* @Transient
* @Id

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
