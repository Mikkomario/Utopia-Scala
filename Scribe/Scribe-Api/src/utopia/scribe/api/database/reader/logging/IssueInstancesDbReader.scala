package utopia.scribe.api.database.reader.logging

import utopia.scribe.core.model.combined.logging.IssueInstances
import utopia.vault.model.immutable.{Row, Table}
import utopia.vault.model.template.SelectsTables
import utopia.vault.nosql.read.DbReader
import utopia.vault.nosql.read.parse.ParseGroupedRows
import utopia.vault.sql.{JoinType, SqlTarget}

import scala.util.Try

/**
  * Used for reading issue, issue variant & issue occurrence data
  * @author Mikko Hilpinen
  * @since 27.07.2025, v1.2
  */
object IssueInstancesDbReader
	extends DbReader[Seq[IssueInstances]] with ParseGroupedRows[IssueInstances] with SelectsTables
{
	// ATTRIBUTES   -------------------------
	
	private val readIssue = IssueDbReader
	private val readVariant = IssueVariantInstancesDbReader
	
	override lazy val tables: Seq[Table] = table +: readVariant.tables
	// The first join is inner, the other join(s) are left
	override lazy val target: SqlTarget =
		readIssue.target.join(readVariant.table).join(readVariant.tables.tail, JoinType.Left)
	
	
	// IMPLEMENTED  -------------------------
	
	override def table: Table = readIssue.table
	
	override def parseGroup(rows: Seq[Row]): Try[IssueInstances] =
		readIssue(rows.head).map { IssueInstances(_, readVariant(rows)) }
}
