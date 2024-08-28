package utopia.logos.database.factory.url

import utopia.flow.generic.model.immutable.Model
import utopia.logos.database.LogosContext
import utopia.logos.database.storable.url.LinkDbModel
import utopia.logos.model.partial.url.LinkData
import utopia.logos.model.stored.url.Link
import utopia.vault.nosql.factory.row.model.FromValidatedRowModelFactory
import utopia.vault.sql.OrderBy

/**
  * Used for reading link data from the DB
  * @author Mikko Hilpinen
  * @since 20.03.2024, v0.3
  */
object LinkDbFactory extends FromValidatedRowModelFactory[Link]
{
	// COMPUTED	--------------------
	
	/**
	  * Model that specifies how the data is read
	  */
	def model = LinkDbModel
	
	
	// IMPLEMENTED	--------------------
	
	override def defaultOrdering: Option[OrderBy] = None
	
	override def table = model.table
	
	override protected def fromValidatedModel(valid: Model) = 
		Link(valid(this.model.id.name).getInt, LinkData(valid(this.model.pathId.name).getInt, 
			valid(this.model.queryParameters.name).notEmpty match {
				case Some(v) => LogosContext.jsonParser.valueOf(v.getString).getModel
				case None => Model.empty
			},
			valid(this.model.created.name).getInstant))
}

