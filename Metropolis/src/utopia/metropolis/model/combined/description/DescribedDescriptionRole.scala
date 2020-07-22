package utopia.metropolis.model.combined.description

import utopia.flow.datastructure.immutable.{Model, Value}
import utopia.flow.datastructure.template
import utopia.flow.datastructure.template.Property
import utopia.flow.generic.{FromModelFactory, ModelConvertible}
import utopia.flow.generic.ValueConversions._
import utopia.metropolis.model.enumeration.DescriptionRole
import utopia.metropolis.model.stored.description.DescriptionLink

/**
  * Combines a description role with some descriptions
  * @author Mikko Hilpinen
  * @since 20.5.2020, v1
  */
case class DescribedDescriptionRole(role: DescriptionRole, descriptions: Set[DescriptionLink]) extends ModelConvertible
{
	// Uses role json keys to describe the roles (so that the user won't need to use recursion
	// or something to interpret the ids)
	override def toModel =
	{
		val descriptionProperties = descriptions.groupBy { _.description.role }.map { case (role, links) =>
			val valueModels = links.map { link => Model(Vector(
				"text" -> link.description.text, "language_id" -> link.description.languageId, "role_id" -> role.id)) }
			// Adds the properties either in singular (Eg. name: {...} or plural Eg. names: [...]) format
			if (valueModels.size == 1)
				role.jsonKey -> (valueModels.head: Value)
			else
				role.jsonKeyPlural -> (valueModels.toVector: Value)
		}.toVector
		Model(("id" -> (role.id: Value)) +: descriptionProperties)
	}
}
