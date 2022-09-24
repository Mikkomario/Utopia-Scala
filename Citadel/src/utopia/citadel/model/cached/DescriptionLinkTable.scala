package utopia.citadel.model.cached

import utopia.citadel.model.cached.DescriptionLinkTable.descriptionLinkAttName
import utopia.flow.view.template.Extender
import utopia.vault.model.immutable.Table

object DescriptionLinkTable
{
	/**
	  * Name of the property that refers to the linked description
	  */
	val descriptionLinkAttName = "descriptionId"
}

/**
  * Represents a table used for accessing links between descriptions and the described items
  * @author Mikko Hilpinen
  * @since 23.10.2021, v2.0
  */
case class DescriptionLinkTable(wrapped: Table, targetLinkAttName: String) extends Extender[Table]
{
	/**
	  * Column that refers to the described item
	  */
	lazy val targetLinkColumn = wrapped(targetLinkAttName)
	/**
	  * Column that refers to the linked description
	  */
	lazy val descriptionLinkColumn = wrapped(descriptionLinkAttName)
}
