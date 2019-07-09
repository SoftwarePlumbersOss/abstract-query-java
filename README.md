# ![Software Plumbers](http://docs.softwareplumbers.com/common/img/SquareIdent-160.png) Abstract Query

Abstract query, providing for client-side optimisation of queries.

## Example

```java

Query query = Query
	.fromJson("{ 
		course: 'javascript 101', 
		student: { age : [21,] }, 
		grade: [,'C']
	}").union("{ 
		course: 'medieval French poetry', 
		student: { age: [40,65]}, 
		grade: [,'C']
	}")

String expr = query.toExpression();
```

and expression should equal:

`grade<"C" and (course="javascript 101" and student.age>=21 or course="medieval French poetry" and student.age>=40 and student.age<65)`

Note that the common expression `grade<"C"` has been factored out of the 'or'. Plainly that's not all that much use in this simple example but when programatically constructing complex queries it is extremely useful to ensure that the query that ultimately sent to the data store is reasonably concise.

## Expression Formatters

The `toExpression` method takes a formatter object so that query objects can be used to create any kind of output. For example:

```java
Formatter<String> formatter = new Formatter<String>() {
	andExpr(...ands) { return ands.join(' && ') }
	orExpr(...ors) { return "(" + ors.join(' || ') + ")"}
	operExpr(dimension, operator, value, context) { 
		return (operator === 'contains')
			? dimension"[" + value + "]"
			: dimension + operator +value 
	}
}

String expr = query.toExpression(formatter)
```

Will result in an expr like: 

`grade<"C" && (course="javascript 101" && student[age>="21"] || course="medieval French poetry" && student[age>="40" && age<"65"])`

The objective is to provide several different expression formatters, to support (at a minumum) constructing suitable expressions for IndexedDB, MongoDB, and MySQL. These formatters will be provided in separate packages so that a code can be written to the abstract-query API without creating a dependency on any given back-end store. The following are currently available:

| Target Language | Package |
|-----------------|---------|
| MongoDB         | [mongo-query-format](https://projects.softwareplumbers.com/common-java/mongo-query-format) |

## Filtering Arrays and Iterables

Abstract Query itself provides a simple 'predicate' property that can be used to filter streams. For example Person is a bean with name and age properties:

```java

List<Person> data = Arrays.asList( 
    new Person("jonathan", 14), 
    new Person("cindy", 18), 
    new Person("ada", 21) 
);

Query query = Query.from("{ age: [,18]}");
List<Person> result = data
    .stream()
    .filter(query.predicate)
    .collect(Collectors.toList());
```

Will filter all the items with age less than 18 from the given data array. While the query API offers little advantage over an anonymous predicate function in this simple example, the ability to compose, optimise, and parametrize queries is a significant benefit in more complex cases. As more expression formatters are built, the ability to use a single query format across native data structures, front-end data stores, and back-end data stores will provide significant benefits to code readability and portability.

## Parameters

Of course, abstract query also supports parametrized queries.

```java
Query query = Query
    .from("{ course: 'javascript 101', student: { age : [ {$:min_age},] }, grade: [,'C']}")
    .or("{ course: 'medieval French poetry', student: { age: [{$:min_age}, 65]}, grade: [,'C']}")

String expr = query.toExpression();
```

Will result in an expr like:

`grade<"C" and (course="javascript 101" and student.age>=$min_age or course="medieval French poetry" and student.age>=$min_age and student.age<65)`

Parameters can be bound to create a new query, thus given the query above:

```java
String expr2 = query
	.bind("min_age",27)
	.toExpression();
```

Will result in an expr like:

`grade<"C" and (course="javascript 101" and student.age>=27 or course="medieval French poetry" and student.age>=27 and student.age<65)`

The library re-optimises the query when parameters are bound, and also tries quite hard to indentify redundant or mutually exclusive criteria even if a query is parametrised.

## Subqueries and Child Objects

Subqueries can be used to put conditions on sub-properties. In the below example, the subquery 'expertise_query' is used to pick items in the data array which have an object in 'expertise' which has a language property of 'java'. 

```java
List<Developer> data = Arrays.toList( 
    new Developer() {{ 	
        name = "jonathan";
    	age = 45;
    	expertise = Arrays.toList( 
    		new Expertise() {{ language="java", level="expert" }}, 
    		new Expertise() {{ language="javascript", level="novice" }
    	}
    }}, ...other entries...
);


List<Developer> result = data.stream()
    .filter(Query
	    .from("{ age: [,50], expertise: { $has : { language: 'java' }")
	    .predicate)
    .collect(Collectors.toList());
```

Subquery syntax can also be used to filter on properties that are not arrays:

```java
List<Developer> data = Arrays.toList( 
    new Developer() { 	
        name= "jonathan";
    	age= 45;
    	expertise= new Expertise() {{ language="java"; level="expert" }}
    }, ...other entries...
);

List<Developer> result = data.stream().filter(Query
	.from("{ age: [,50], expertise: { language: 'java' }")
	.predicate)
    .collect(Collectors.toList());
```

## Caching

Abstract query will also aid in building any kind of caching layer. Because abstract-query actually stores the query in an internal canoncial form, two queries can be compared for equality even if they are outwardly somewhat different. Thus:

```javas
Query query1 = Query.from("{x: [,2], y: { alpha: [2,6], beta: { nuts: 'brazil' }}}");
Query query2 = Query.from("{y: { beta: { nuts: 'brazil' }, alpha: [2,6]}, x: [,2]}");
Query query3 = query1.and(query2);
Query query4 = query2.and(query1);

query1.equals(query2) // true
query3.equals(query4) // true
```

Even better, query.contains allows you to detect whether one query is a subset of another; thus data can be potentially be retrieved by just filtering an existing cached result set rather than requerying the data store for data we already have.

For the latest API documentation see [The Software Plumbers Site](http://docs.softwareplumbers.com/abstract-query/master)

## Project Status

Alpha
