package utopia.scribe.api.database.reader.management

import utopia.scribe.api.database.storable.management.{CommentDbModel, ResolutionDbModel}
import utopia.scribe.core.model.combined.management.DetailedResolution
import utopia.vault.model.enumeration.SelectTarget
import utopia.vault.model.immutable.{Row, Table}
import utopia.vault.nosql.read.DbRowReader
import utopia.vault.sql.{JoinType, SqlTarget}

import scala.util.Try

/**
 * Used for pulling detailed issue resolution data from the DB
 *
 * @author Mikko Hilpinen
 * @since 26.08.2025, v1.0.6
 */
object DetailedResolutionDbReader extends DbRowReader[DetailedResolution]
{
	// ATTRIBUTES   --------------------------
	
	private val readResolution = ResolutionDbReader
	private val readNotification = IssueNotificationDbReader
	
	private val resolution = ResolutionDbModel
	private val comment = CommentDbModel
	
	override lazy val tables: Seq[Table] = Vector(readResolution.table, comment.table, readNotification.table)
	override lazy val target: SqlTarget =
		readResolution.target.join(readNotification.table, JoinType.Left)
			.join(resolution.commentId.column, JoinType.Left)
	
	override lazy val selectTarget: SelectTarget =
		readResolution.selectTarget + readNotification.selectTarget + comment.text.column
	
	
	// IMPLEMENTED  --------------------------
	
	override def table: Table = readResolution.table
	
	override def shouldParse(row: Row): Boolean = readResolution.shouldParse(row)
	override def apply(row: Row): Try[DetailedResolution] = readResolution(row).map { resolution =>
		DetailedResolution(resolution, row(comment.text).getString, readNotification.tryParse(row))
	}
}
