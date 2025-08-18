package utopia.vault.nosql.read

import utopia.vault.model.enumeration.SelectTarget
import utopia.vault.model.immutable.Table
import utopia.vault.model.mutable.ResultStream
import utopia.vault.model.template.{HasSelectTarget, HasTable, HasTables}
import utopia.vault.nosql.read.DbReader.MappingDbReader
import utopia.vault.nosql.read.parse.ParseResultStream
import utopia.vault.sql.SqlTarget

object DbReader
{
	// NESTED   ----------------------------
	
	private class MappingDbReader[-A, +B](reader: DbReader[A], f: A => B) extends DbReader[B]
	{
		override def selectTarget: SelectTarget = reader.selectTarget
		override def table: Table = reader.table
		override def tables: Seq[Table] = reader.tables
		override def target: SqlTarget = reader.target
		
		override def apply(stream: ResultStream): B = f(reader(stream))
	}
}

/**
  * Common trait for interfaces which target and parse specific database data
  * @author Mikko Hilpinen
  * @since 10.07.2025, v1.22
  */
trait DbReader[+A] extends ParseResultStream[A] with HasSelectTarget with HasTable with HasTables
{
	/**
	 * @param f A mapping function applied to the pulled results
	 * @tparam B Type of mapping results
	 * @return A reader that maps the results of this reader
	 */
	def mapResult[B](f: A => B): DbReader[B] = new MappingDbReader[A, B](this, f)
}