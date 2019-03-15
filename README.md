# SlimORM
SlimORM is a lightweight Java ORM library, based on JDBC.

Why yet another ORM library, when we have popular ORM libraries already?
Because sometimes, You just don't want the complexity of those popular libraries.
Instead, You need basic CRUD operations, which would be simple to set up, simple to use and have minimal [dependencies](#dependencies).
For large and complex enterprise projects, SlimORM probably lacks features and flexibility.

# Basic Usage

Establish database link:

```java
Database db = new Database("org.postgresql.Driver", "jdbc:postgresql://localhost:5432/demoDB", "demouser", "password");
```

Insert a new record:

```java
Employee employee = new Employee();
employee.name = "John Smith";
employee.dateOfBirth = LocalDate.of(1992, 3, 27);
db.insert(employee);
```

Update an existing record:

```java
employee.dateOfBirth = LocalDate.of(1993, 5, 14);
db.update(employee);
```

Delete the record (based on id):

```java
db.delete(Employee.class, employee.id);
```

Get a specific record from database (based on id):

```java
Employee employee = db.getById(Employee.class, id);
```

Get a list of records with SQL filter:

```java
Collection<Employee> employees = db.where("name LIKE ?", "A%").list(Employee.class);
```

# Dependencies

Add SlimORM dependency into build.gradle:

```gradle
dependencies {
    implementation 'eu.miltema:slimorm:1.0.0'
}
```

or alternatively, if using Maven, then into pom.xml:

```xml
<dependencies>
  <dependency>
    <groupId>eu.miltema</groupId>
    <artifactId>slimorm</artifactId>
    <version>1.0.0</version>
  </dependency>
</dependencies>
```

In addition, build.gradle or pom.xml must refer to database driver. For example, when using PostgreSQL, build.gradle contains:

```gradle
dependencies {
    implementation 'eu.miltema:slimorm:1.0.0'
    runtime 'org.postgresql:postgresql:42.2.5'
}
```

SlimORM itself depends on 2 libraries: javax.persistence and com.google.code.gson:gson. These are resolved by build system automatically.

# Annotations

SlimORM supports these javax.persistence annotations when declaring entities:
* **@Table** - without this annotation, SlimORM uses snake-case class name as table name. For example, class EmployeeDetails would be stored into table employee\_details
* **@Column** - without this annotation, SlimORM uses snake-case field name as column name. For example, field dateOfBirth would be stored into column date\_of\_birth
* **@Transient** - Annotation @Transient and Java modifier transient have the same effect: SlimORM will not read/write this field to the database
* **@Id**

For example:

```java
@Table(name="employees")
public class Employee {
	@Id
	int id;

	String name;

	@Column(name = "dob")
	LocalDate dateOfBirth;

	@Transient
	boolean isDirty;

	transient boolean isUpToDate;
}
```

# Transactions

In the "Basic Usage" chapter, each example statement was executed as individual transaction. Here is an example with a transaction including multiple statements.
When all statements succeed, SlimORM automatically commits the transaction. When any of the statements throws an Exception, SlimORM automatically rolls back the entire transaction.

```java
Employee[] entities = db.transaction((db, connection) -> {
	Employee e1 = new Employee();
	e1.name = "John Smith";
	db.insert(e1);
	Employee e2 = new Employee();
	e2.name = "Jane Doe";
	db.insert(e2);
	return new Employee[] {e1, e2};
});
```

SlimORM manages the connection itself - no need to close, commit or rollback the connection.
It is possible to return an object of any type from the transaction.

# SQL Dialects

By default, SlimORM uses PostgreSQL dialect. If that dialect is causing problems, You must implement a custom dialect and a superclass of Database:

```java
public class MySqlDialect extends DefaultDialect {
	... // override any methods that cause problems
}
public class MySqlDatabase extends Database {
	@Override
	public Dialect getDialect() {
		return MySqlDialect();
	}
}
```

And when establishing database link, don't forget to use Your custom database class instead of Database:

```java
Database db = new MySqlDatabase(dbDriver, dbUrl, dbUser, dbPassword);
```

# Data Types

The developer is responsible of selecting Java type and corresponding SQL data byte, which must match. By default, SlimORM supports these Java types by default:
String, byte, Byte, short, Short, int, Integer, long, Long, float, Float, double, Double, BigDecimal, byte[], Timestamp, Instant, Date, LocalDate, LocalDateTime, ZonedDateTime.

Be aware that PostgreSQL does not store timezone id into record (even when data type is _with time zone_). Therefore, all time-related columns store correct instant in time, but have lost the original timezone id.

For data types not listed above, one must superclass DefaultDialect and provide custom saveBinder and loadBinder. 

# Deploying to Maven Central
* Install GnuPG from https://www.gnupg.org/download/ .
* In environment, set GRADLE\_USER\_HOME=C:/directory-for-gradle.properties

* File gradle.properties should look like this:

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


# History

I first started with a custom ORM library probably around 2003, mostly for PostgreSQL.
Then at some point I found https://github.com/dieselpoint/norm, which had pretty similar logic and API to my own library.
Norm was written for MySQL and worked rather well with PostgreSQL, although multiple issues emerged.
Some of those issues I was able to handle and I used Norm in many projects. But a number of issues still remained unsolved. 
Finally I decided, that it is easier to continue with my own library - SlimORM, which now has some Norm influences.
