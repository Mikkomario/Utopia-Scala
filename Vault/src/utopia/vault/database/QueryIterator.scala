package utopia.vault.database

import utopia.vault.model.immutable.Result
import utopia.vault.sql.{Limit, Offset, SqlSegment}

/**
 * An iterator that is based on queries using limit and offset parameters. Useful when dealing with
 * very large tables. Returns a limited number of rows on each iteration. The last iteration may return
 * zero rows when a) the query itself returns 0 rows or b) the query returns a number of rows equally divided
 * by 'rowsPerIteration' (E.g. 10 rows in database and using 'rowsPerIteration' of 5 will yield 3 results,
 * last of which is empty)
 * @author Mikko Hilpinen
 * @since 30.4.2021, v1.7.1
 * @param baseQuery The query that is performed on each iteration, not including limit or offset
 * @param rowsPerIteration Maximum number of rows returned on each call of .next()
 * @param connection Database connection (implicit). The connection must be kept open during the use of
 *                   this iterator.
 */
class QueryIterator(baseQuery: SqlSegment, rowsPerIteration: Int)(implicit connection: Connection)
	extends Iterator[Result]
{
	// ATTRIBUTES   --------------------------
	
	private var nextOffset = 0
	private var closedFlag = false
	
	
	// IMPLEMENTED  --------------------------
	
	override def hasNext = !closedFlag
	
	override def next() =
	{
		// Performs the next query
		val result = connection(baseQuery + Limit(rowsPerIteration) + Offset(nextOffset))
		
		// Checks whether more results should be expected
		if (result.rows.size < rowsPerIteration)
			closedFlag = true
		nextOffset += rowsPerIteration
		
		result
	}
}
