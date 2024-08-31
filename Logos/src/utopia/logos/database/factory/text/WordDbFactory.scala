package utopia.logos.database.factory.text

import utopia.flow.generic.model.immutable.Model
import utopia.logos.database.storable.text.WordDbModel
import utopia.logos.model.partial.text.WordData
import utopia.logos.model.stored.text.StoredWord
import utopia.vault.nosql.factory.row.model.FromValidatedRowModelFactory
import utopia.vault.sql.OrderBy

/**
  * Used for reading word data from the DB
  * @author Mikko Hilpinen
  * @since 27.08.2024, v0.3
  */
object WordDbFactory extends FromValidatedRowModelFactory[StoredWord]
{
	// COMPUTED	--------------------
	
	/**
	  * Model that specifies how the data is read
	  */
	def model = WordDbModel
	
	
	// IMPLEMENTED	--------------------
	
	override def defaultOrdering: Option[OrderBy] = None
	
	override def table = model.table
	
	override protected def fromValidatedModel(valid: Model) = 
		StoredWord(valid(this.model.id.name).getInt, WordData(valid(this.model.text.name).getString,
			valid(this.model.created.name).getInstant))
}

