package utopia.logos.database.factory.url

import utopia.flow.generic.model.immutable.Model
import utopia.logos.database.{LogosContext, LogosTables}
import utopia.logos.model.partial.url.LinkData
import utopia.logos.model.stored.url.Link
import utopia.vault.nosql.factory.row.model.FromValidatedRowModelFactory

/**
  * Used for reading link data from the DB
  * @author Mikko Hilpinen
  * @since 20.03.2024, v1.0
  */
object LinkDbFactory extends FromValidatedRowModelFactory[Link]
{
	// IMPLEMENTED	--------------------
	
	override def defaultOrdering = None
	
	override def table = LogosTables.link
	
	override protected def fromValidatedModel(valid: Model) =
		Link(valid("id").getInt, LinkData(valid("requestPathId").getInt,
			valid("queryParameters").notEmpty match {
				case Some(v) => LogosContext.jsonParser.valueOf(v.getString).getModel
				case None => Model.empty
			},
			valid("created").getInstant))
}

