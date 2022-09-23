package utopia.citadel.database.factory.description

import utopia.citadel.model.cached.DescriptionLinkTable
import utopia.flow.collection.value.typeless.Model
import utopia.flow.generic.ValueUnwraps._
import utopia.metropolis.model.partial.description.DescriptionLinkData
import utopia.metropolis.model.stored.description.DescriptionLink
import utopia.vault.nosql.factory.row.model.FromValidatedRowModelFactory

/**
  * Used for reading description links from the DB - table from which the links are read may vary
  * @author Mikko Hilpinen
  * @since 23.10.2021, v2.0
  */
case class DescriptionLinkFactory(linkTable: DescriptionLinkTable)
	extends FromValidatedRowModelFactory[DescriptionLink]
{
	override def table = linkTable.wrapped
	
	override def defaultOrdering = None
	
	override protected def fromValidatedModel(model: Model) =
		DescriptionLink(model("id"),
			DescriptionLinkData(model(linkTable.targetLinkAttName), model(DescriptionLinkTable.descriptionLinkAttName)))
}