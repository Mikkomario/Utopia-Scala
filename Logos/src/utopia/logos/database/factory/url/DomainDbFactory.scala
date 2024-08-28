package utopia.logos.database.factory.url

import utopia.flow.generic.model.immutable.Model
import utopia.logos.database.storable.url.DomainDbModel
import utopia.logos.model.partial.url.DomainData
import utopia.logos.model.stored.url.Domain
import utopia.vault.nosql.factory.row.model.FromValidatedRowModelFactory
import utopia.vault.sql.OrderBy

/**
  * Used for reading domain data from the DB
  * @author Mikko Hilpinen
  * @since 20.03.2024, v0.3
  */
object DomainDbFactory extends FromValidatedRowModelFactory[Domain]
{
	// COMPUTED	--------------------
	
	/**
	  * Model that specifies how the data is read
	  */
	def model = DomainDbModel
	
	
	// IMPLEMENTED	--------------------
	
	override def defaultOrdering: Option[OrderBy] = None
	
	override def table = model.table
	
	override protected def fromValidatedModel(valid: Model) = 
		Domain(valid(this.model.id.name).getInt, DomainData(valid(this.model.url.name).getString, 
			valid(this.model.created.name).getInstant))
}

