package utopia.scribe.api.database.reader.logging

import utopia.scribe.api.database.reader.management.{DetailedResolutionDbReader, IssueAliasDbReader}
import utopia.scribe.api.database.storable.management.{IssueNotificationDbModel, ResolutionDbModel}
import utopia.scribe.core.model.combined.logging.ManagedIssue
import utopia.vault.model.enumeration.SelectTarget
import utopia.vault.model.immutable.{Row, Table}
import utopia.vault.nosql.read.DbReader
import utopia.vault.nosql.read.parse.ParseGroupedRows
import utopia.vault.sql.{JoinType, SqlTarget}

import scala.util.Try

/**
 * Pulls issue data from the DB, including management-related information
 *
 * @author Mikko Hilpinen
 * @since 27.08.2025, v1.1
 */
object ManagedIssueDbReader extends DbReader[Seq[ManagedIssue]] with ParseGroupedRows[ManagedIssue]
{
	// ATTRIBUTES   ---------------------------
	
	private val readIssue = IssueDbReader
	private val readAlias = IssueAliasDbReader
	private val readResolution = DetailedResolutionDbReader
	
	private val resolution = ResolutionDbModel
	private val notification = IssueNotificationDbModel
	
	override lazy val target: SqlTarget = readIssue.target
		.join(Vector(readAlias.table, resolution.table, notification.table), JoinType.Left)
		.join(resolution.commentId.column, JoinType.Left)
	
	override lazy val selectTarget: SelectTarget =
		readIssue.selectTarget + readAlias.selectTarget + readResolution.selectTarget
	
	
	// IMPLEMENTED  ---------------------------
	
	override def table: Table = readIssue.table
	override def tables: Seq[Table] = target.tables
	
	override def parseGroup(rows: Seq[Row]): Try[ManagedIssue] = {
		val primaryRow = rows.head
		readIssue(primaryRow).map { issue =>
			val alias = readAlias.tryParse(primaryRow)
			val resolutions = readResolution(rows)
			ManagedIssue(issue, alias, resolutions)
		}
	}
}
