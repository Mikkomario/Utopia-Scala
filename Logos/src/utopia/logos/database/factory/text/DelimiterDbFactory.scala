package utopia.logos.database.factory.text

import utopia.flow.generic.model.immutable.Model
import utopia.logos.database.storable.text.DelimiterDbModel
import utopia.logos.model.partial.text.DelimiterData
import utopia.logos.model.stored.text.Delimiter
import utopia.vault.nosql.factory.row.model.FromValidatedRowModelFactory
import utopia.vault.sql.OrderBy

/**
  * Used for reading delimiter data from the DB
  * @author Mikko Hilpinen
  * @since 27.08.2024, v0.3
  */
object DelimiterDbFactory extends FromValidatedRowModelFactory[Delimiter]
{
	// COMPUTED	--------------------
	
	/**
	  * Model that specifies how the data is read
	  */
	def model = DelimiterDbModel
	
	
	// IMPLEMENTED	--------------------
	
	override def defaultOrdering: Option[OrderBy] = None
	
	override def table = model.table
	
	override protected def fromValidatedModel(valid: Model) = 
		Delimiter(valid(this.model.id.name).getInt, DelimiterData(valid(this.model.text.name).getString, 
			valid(this.model.created.name).getInstant))
}

