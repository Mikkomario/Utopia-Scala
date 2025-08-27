package utopia.scribe.api.database.reader.management

import utopia.flow.generic.model.immutable.Model
import utopia.flow.util.Version
import utopia.scribe.api.database.storable.management.ResolutionDbModel
import utopia.scribe.core.model.partial.management.ResolutionData
import utopia.scribe.core.model.stored.management.Resolution
import utopia.vault.model.template.HasTableAsTarget
import utopia.vault.nosql.read.DbRowReader
import utopia.vault.nosql.read.parse.ParseTableModel

import scala.util.Success

/**
  * Used for reading resolution data from the DB
  * @author Mikko Hilpinen
  * @since 26.08.2025, v1.2
  */
object ResolutionDbReader 
	extends DbRowReader[Resolution] with ParseTableModel[Resolution] with HasTableAsTarget
{
	// ATTRIBUTES	--------------------
	
	/**
	  * Model that specifies how the data is read
	  */
	val model = ResolutionDbModel
	
	
	// IMPLEMENTED	--------------------
	
	override def table = model.table
	
	override def fromValid(valid: Model) = 
		Success(Resolution(valid(this.model.id.name).getInt, ResolutionData(
			resolvedIssueId = valid(this.model.resolvedIssueId.name).getInt,
			commentId = valid(this.model.commentId.name).int, 
			versionThreshold = valid(this.model.versionThreshold.name).string.map(Version.apply),
			created = valid(this.model.created.name).getInstant, 
			deprecates = valid(this.model.deprecates.name).instant, 
			silences = valid(this.model.silences.name).getBoolean, 
			notifies = valid(this.model.notifies.name).getBoolean)))
}

