package utopia.metropolis.model.combined.description

import utopia.flow.datastructure.immutable.{Constant, Model, Value}
import utopia.flow.generic.ValueConversions._
import utopia.metropolis.model.partial.description.DescriptionData
import utopia.metropolis.model.stored.description.DescriptionRole

object SimplyDescribed
{
	/**
	  * Converts a set of descriptions into a set of properties
	  * @param descriptions A set of descriptions
	  * @param roles Description roles to use
	  * @return A set of constants based on the roles + descriptions
	  */
	def descriptionPropertiesFrom(descriptions: Iterable[DescriptionData], roles: Iterable[DescriptionRole]) =
	{
		roles.map { role =>
			val roleDescriptions = descriptions.filter { _.roleId == role.id }
			// Case: There are multiple descriptions for a single role => Lists values and languages
			if (roleDescriptions.size > 1)
				Constant(role.jsonKeyPlural, roleDescriptions.map { description =>
					Model(Vector("text" -> description.text, "language_id" -> description.languageId))
				}.toVector)
			// Case: There is only a single description => uses a simple string value
			else
				Constant(role.jsonKeySingular, roleDescriptions.headOption match {
					case Some(description) => description.text
					case None => Value.empty
				})
		}
	}
}

/**
  * A common trait for instances which include descriptions while supporting a simpler
  * model structuring
  * @author Mikko Hilpinen
  * @since 29.6.2021, v1.0.1
  */
trait SimplyDescribed extends Described with DescribedSimpleModelConvertible
{
	// ABSTRACT --------------------------
	
	/**
	  * Forms a simple based model
	  * @param roles Available description roles, for reference
	  * @return A simple model based on the wrapped item
	  */
	protected def simpleBaseModel(roles: Iterable[DescriptionRole]): Model[Constant]
	
	
	// IMPLEMENTED    --------------------
	
	/**
	  * Converts this instance to a simple model
	  * @param descriptionRoles Roles of the descriptions to include
	  * @return A model
	  */
	override def toSimpleModelUsing(descriptionRoles: Iterable[DescriptionRole]) =
		simpleBaseModel(descriptionRoles) ++
			SimplyDescribed.descriptionPropertiesFrom(descriptions.map { _.description }, descriptionRoles)
}
