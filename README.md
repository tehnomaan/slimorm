# SlimORM
SlimORM is a lightweight Java ORM library.

Why yet another ORM library, when we have popular ORM libraries already?
Because sometimes, You just don't want the complexity of those popular libraries.
Instead, You need basic CRUD operations, which would be simple to set up, simple to use and have minimal dependencies.
For large and complex enterprise projects, SlimORM probably lacks features and flexibility.

# Basic Usage

Establish database link:

```java
Database db = new Database("org.postgresql.Driver", "jdbc:postgresql://localhost:5432/demoDB", "demouser", "password");
```

Or use this, if You have declared database access via JNDI (for example, in a web application):

```java
Database db = new Database("jdbc/demoDB");
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

Get a specific record (based on id):

```java
Employee employee = db.get(Employee.class, id);
```

Get a list of records with SQL filter:

```java
Collection<Employee> employees = db.where("name LIKE ?", "A%").list(Employee.class);
```

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

# Deploying to Maven Central
* Install GnuPG from https://www.gnupg.org/download/ .
* In environment, set GRADLE\_USER\_HOME=C:/directory-for-gradle.properties

* File gradle.properties should look like this:

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

* Start building

```
gradle build
```

# History

I first started with a custom ORM library probably around 2003, mostly for PostgreSQL.
Then at some point I found https://github.com/dieselpoint/norm, which had pretty similar logic and usage to my own library.
Norm was written for MySQL and worked rather well with PostgreSQL, although multiple issues emerged.
Some of those issues I was able to handle and I used Norm in many projects. But a number of issues still remained unsolved. 
Finally I concluded, that it is easier to continue with my own library - SlimORM.
