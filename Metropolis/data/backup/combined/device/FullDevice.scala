package utopia.metropolis.model.combined.device

import utopia.flow.datastructure.immutable.{Model, ModelDeclaration, PropertyDeclaration}
import utopia.flow.datastructure.template
import utopia.flow.datastructure.template.Property
import utopia.flow.generic.{FromModelFactory, IntType, ModelConvertible}
import utopia.flow.generic.ValueConversions._
import utopia.flow.util.CollectionExtensions._
import utopia.metropolis.model.combined.description.SimplyDescribed
import utopia.metropolis.model.stored.description.{DescriptionLinkOld, DescriptionRole}

object FullDevice extends FromModelFactory[FullDevice]
{
	// ATTRIBUTES	----------------------
	
	private val schema = ModelDeclaration(PropertyDeclaration("id", IntType))
	
	
	// IMPLEMENTED	----------------------
	
	override def apply(model: template.Model[Property]) = schema.validate(model).toTry.flatMap { valid =>
		valid("descriptions").getVector.tryMap { v => DescriptionLinkOld(v.getModel) }.map { descriptions =>
			val userIds = valid("user_ids").getVector.flatMap { _.int }.toSet
			FullDevice(valid("id").getInt, descriptions.toSet, userIds)
		}
	}
}

/**
  * Contains basic device information with descriptions and associated user ids
  * @author Mikko Hilpinen
  * @since 19.6.2020, v1
  */
case class FullDevice(id: Int, descriptions: Set[DescriptionLinkOld], userIds: Set[Int])
	extends ModelConvertible with SimplyDescribed
{
	// IMPLEMENTED	-------------------------
	
	override def toModel = Model(Vector("id" -> id,
		"descriptions" -> descriptions.map { _.toModel }.toVector, "user_ids" -> userIds.toVector))
	
	override protected def simpleBaseModel(roles: Iterable[DescriptionRole]) = Model(Vector(
		"id" -> id, "user_ids" -> userIds.toVector
	))
}
