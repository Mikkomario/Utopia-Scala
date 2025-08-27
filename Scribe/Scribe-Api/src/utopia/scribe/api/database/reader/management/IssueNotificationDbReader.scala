package utopia.scribe.api.database.reader.management

import utopia.flow.generic.model.immutable.Model
import utopia.scribe.api.database.storable.management.IssueNotificationDbModel
import utopia.scribe.core.model.partial.management.IssueNotificationData
import utopia.scribe.core.model.stored.management.IssueNotification
import utopia.vault.model.template.HasTableAsTarget
import utopia.vault.nosql.read.DbRowReader
import utopia.vault.nosql.read.parse.ParseTableModel

import scala.util.Success

/**
  * Used for reading issue notification data from the DB
  * @author Mikko Hilpinen
  * @since 26.08.2025, v1.2
  */
object IssueNotificationDbReader 
	extends DbRowReader[IssueNotification] with ParseTableModel[IssueNotification] with HasTableAsTarget
{
	// ATTRIBUTES	--------------------
	
	/**
	  * Model that specifies how the data is read
	  */
	val model = IssueNotificationDbModel
	
	
	// IMPLEMENTED	--------------------
	
	override def table = model.table
	
	override def fromValid(valid: Model) = 
		Success(IssueNotification(valid(this.model.id.name).getInt, 
			IssueNotificationData(resolutionId = valid(this.model.resolutionId.name).getInt, 
			created = valid(this.model.created.name).getInstant, 
			closed = valid(this.model.closed.name).instant)))
}

