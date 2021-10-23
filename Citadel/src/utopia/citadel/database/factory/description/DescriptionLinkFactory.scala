package utopia.citadel.database.factory.description

import utopia.citadel.model.cached.DescriptionLinkTable
import utopia.flow.datastructure.immutable.{Constant, Model}
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
	
	override protected def fromValidatedModel(model: Model[Constant]) =
		DescriptionLink(model("id"),
			DescriptionLinkData(model(linkTable.targetLinkAttName), model(DescriptionLinkTable.descriptionLinkAttName)))
}