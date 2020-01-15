/**
 * Abstract query, providing for client-side optimisation of queries.
 *
 * ## Example
 *
 * ```java
 * Query query = Query
 *	.fromJson("{ 
 *		course: 'javascript 101',  
 *		student: { age : [21,] }, 
 *		grade: [,'C']
 *	}").union("{ 
 *		course: 'medieval French poetry', 
 *		student: { age: [40,65]},  
 *		grade: [,'C']
 *	}")
 *
 * String expr = query.toExpression(Visitors.SIMPLIFY).toExpression(Visitors.DEFAULT);
 * ```
 */
package com.softwareplumbers.common.abstractquery;
