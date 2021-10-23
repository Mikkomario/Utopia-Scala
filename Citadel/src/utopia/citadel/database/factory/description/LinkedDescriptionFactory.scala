package utopia.citadel.database.factory.description

import utopia.citadel.model.cached.DescriptionLinkTable
import utopia.metropolis.model.combined.description.LinkedDescription
import utopia.metropolis.model.stored.description.{Description, DescriptionLink}
import utopia.vault.nosql.factory.row.linked.CombiningFactory
import utopia.vault.nosql.template.Deprecatable

object LinkedDescriptionFactory
{
	/**
	  * @param linkTable Table that contains description links
	  * @return Factory for reading linked descriptions using that link table
	  */
	def apply(linkTable: DescriptionLinkTable) = apply(DescriptionLinkFactory(linkTable))
}

/**
  * Used for reading descriptions with their links included
  * @author Mikko Hilpinen
  * @since 23.10.2021, v2.0
  */
case class LinkedDescriptionFactory(linkFactory: DescriptionLinkFactory)
	extends CombiningFactory[LinkedDescription, Description, DescriptionLink] with Deprecatable
{
	override def parentFactory = DescriptionFactory
	
	override def childFactory = linkFactory
	
	override def nonDeprecatedCondition = parentFactory.nonDeprecatedCondition
}
