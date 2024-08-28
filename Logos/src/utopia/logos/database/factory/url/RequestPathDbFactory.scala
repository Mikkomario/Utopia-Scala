package utopia.logos.database.factory.url

import utopia.flow.generic.model.immutable.Model
import utopia.logos.database.storable.url.RequestPathDbModel
import utopia.logos.model.partial.url.RequestPathData
import utopia.logos.model.stored.url.RequestPath
import utopia.vault.nosql.factory.row.model.FromValidatedRowModelFactory
import utopia.vault.sql.OrderBy

/**
  * Used for reading request path data from the DB
  * @author Mikko Hilpinen
  * @since 20.03.2024, v0.3
  */
object RequestPathDbFactory extends FromValidatedRowModelFactory[RequestPath]
{
	// COMPUTED	--------------------
	
	/**
	  * Model that specifies how the data is read
	  */
	def model = RequestPathDbModel
	
	
	// IMPLEMENTED	--------------------
	
	override def defaultOrdering: Option[OrderBy] = None
	
	override def table = model.table
	
	override protected def fromValidatedModel(valid: Model) = 
		RequestPath(valid(this.model.id.name).getInt, RequestPathData(valid(this.model.domainId.name).getInt, 
			valid(this.model.path.name).getString, valid(this.model.created.name).getInstant))
}

