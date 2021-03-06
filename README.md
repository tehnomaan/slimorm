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

Insert a new [Employee-record](#annotations):

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
    implementation 'eu.miltema:slimorm:1.3.2'
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
    implementation 'eu.miltema:slimorm:1.2.1'
    runtime 'org.postgresql:postgresql:42.2.5'
}
```

SlimORM itself depends on 2 libraries: javax.persistence and com.google.code.gson:gson. These are resolved by build system automatically.

# Annotations

SlimORM supports these javax.persistence annotations when declaring entities:
* **@Table (name)** - without this annotation, SlimORM uses snake-case class name as table name. For example, class EmployeeDetails would be stored into table employee\_details
* **@Column (name, updatable, insertable)** - without this annotation, SlimORM uses snake-case field name as column name. For example, field dateOfBirth would be stored into column date\_of\_birth
* **@Transient** - annotation @Transient and Java modifier transient have the same effect: SlimORM will not read/write this field to the database
* **@Id** - declares a primary key field. Only single-field primary keys are supported - composite primary keys are not
* **@GeneratedValue** - database generates the value for this field. INSERT & UPDATE will not modify this field
* **ManyToOne** - defines a many-to-one relationship. In database, this field must be a foreign key field to target entity table.
    When writing, only the foreign key is persisted (the referenced entity has to be persisted expicitly beforehand).
    When reading, only referenced entity id is filled. 
* **@JSon** - declares that this field will be stored as a JSon object. This is not a javax.persistence annotation, but SlimORM annotation

For example:

```java
@Table(name="employees")
public class Employee {
	@Id
	@GeneratedValue
	int id;

	String name;

	@Column(name = "dob")
	LocalDate dateOfBirth;

	@Transient
	boolean isDirty;

	transient boolean isDirty2;

	@JSon
	Contract[] contracts;

	@ManyToOne
	Department department;
}
```

NB! Since most database designs have an auto-generated primary key with name _id_, then SlimORM has a special shorthand: a field with name _id_ and without @Id is still treated as if both annotations were present.
If this is not what You need, declare Your primary key with a different name or add @Id annotation to a different field.

# @ManyToOne and Foreign Keys

In the Employee case above, table employees must contain column _department_, which is a foreign key field, referencing table _department_.
* When writing Employee-entity to database, only the foreign key is persisted (the referenced entity itself has to be persisted expicitly beforehand)
* When reading Employee-entity from database, only referenced entity id is filled inside Department-object (for performance reasons)
* To load Employee entities with fully initialized Department-entites, use method _referencedColumns_

```java
		db.where("id=?", id).referencedColumns("*").list(Employee.class);//load all columns for all referenced entities
		db.where("id=?", id).
			referencedColumns(Department.class, "*").//load all columns for Department references
			referencedColumns("id, name").//load id, name columns for all other references
			list(Employee.class);
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
public class MySqlDialect extends PgDialect {
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

For data types not listed above, one must superclass PgDialect and provide custom saveBinder and loadBinder. 

# Logging

To keep the amount of dependencies low, SlimORM is not logging automatically. To add logging to SlimORM (System.out, log4j, slf etc), do this:

```java
Database db = new Database(...).setLogger(message -> System.out.println(message));
```

# Authorization and Record-Level Restrictions

To inject restrictions into all database-related queries (for example restrict to a specific account only), extend and use a custom database class (for example SecureDatabase) like this:

```java
public class SecureDatabase extends Database {

	private int accountId;

	public SecureDatabase(String jndiName, int accountId) {
		super(jndiName);
		this.accountId = accountId;
	}

	// inject additional account-id filter into every query
	@Override
	protected String injectIntoWhereExpression(Class<?> entityClass, String whereExpression) {
		return (whereExpression == null ? "account_id=?" : "(" + whereExpression + ") AND account_id=?");
	}

	// inject account-id filtering parameter into every query
	@Override
	protected Object[] injectWhereParameters(Class<?> entityClass, Object[] whereParameters) {
		if (whereParameters != null) {
			whereParameters = Arrays.copyOf(whereParameters, whereParameters.length + 1);
			whereParameters[whereParameters.length - 1] = accountId;
			return whereParameters;
		}
		else return new Object[] {accountId};
	}

	@Override
	protected void authorize(Object entity) throws UnauthorizedException {
		if (!/* entity validation logic */)
			throw new UnauthorizedException();
	}
}

Database db = new SecureDatabase("java:comp/env/jdbc/demodb", accountId);
```

# History

I first started with a custom ORM library probably around 2003, mostly for PostgreSQL.
Then at some point I found https://github.com/dieselpoint/norm, which had pretty similar logic and API to my own library.
Norm was written for MySQL and worked rather well with PostgreSQL, although multiple issues emerged.
Some of those issues I was able to handle and I used Norm in many projects. But a number of issues still remained unsolved. 
Finally I decided, that it is easier to continue with my own library - SlimORM, which now has some Norm influences.
