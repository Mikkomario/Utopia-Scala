package utopia.scribe.api.database.reader.logging

import utopia.bunnymunch.jawn.JsonBunny
import utopia.flow.collection.immutable.range.Span
import utopia.flow.generic.model.immutable.Model
import utopia.scribe.api.database.storable.logging.IssueOccurrenceDbModel
import utopia.scribe.core.model.partial.logging.IssueOccurrenceData
import utopia.scribe.core.model.stored.logging.IssueOccurrence
import utopia.vault.model.template.HasTableAsTarget
import utopia.vault.nosql.read.DbRowReader
import utopia.vault.nosql.read.parse.ParseTableModel

import scala.util.Success

/**
  * Used for reading issue occurrence data from the DB
  * @author Mikko Hilpinen
  * @since 27.07.2025, v0.1
  */
object IssueOccurrenceDbReader 
	extends DbRowReader[IssueOccurrence] with ParseTableModel[IssueOccurrence] with HasTableAsTarget
{
	// ATTRIBUTES	--------------------
	
	/**
	  * Model that specifies how the data is read
	  */
	val model = IssueOccurrenceDbModel
	
	
	// IMPLEMENTED	--------------------
	
	override def table = model.table
	
	override def fromValid(valid: Model) = 
		Success(IssueOccurrence(valid(this.model.id.name).getInt, 
			IssueOccurrenceData(valid(this.model.caseId.name).getInt, 
			valid(this.model.errorMessages.name).notEmpty match { case Some(v) => JsonBunny.sureMunch(v.getString).getVector.map { v => v.getString }; case None => Vector.empty }, 
			valid(this.model.details.name).notEmpty match { case Some(v) => JsonBunny.sureMunch(v.getString).getModel; case None => Model.empty }, 
			valid(this.model.count.name).getInt, Span(valid(this.model.earliest.name).getInstant, 
			valid(this.model.latest.name).getInstant))))
}

