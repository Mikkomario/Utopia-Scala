package utopia.vault.test.model.partial.item

import utopia.flow.collection.immutable.Single
import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.generic.factory.FromModelFactoryWithSchema
import utopia.flow.generic.model.immutable.{Model, ModelDeclaration, PropertyDeclaration}
import utopia.flow.generic.model.mutable.DataType.InstantType
import utopia.flow.generic.model.mutable.DataType.StringType
import utopia.flow.generic.model.template.ModelConvertible
import utopia.flow.time.Now
import utopia.vault.test.model.factory.item.VersionedTestItemFactory

import java.time.Instant

object VersionedTestItemData extends FromModelFactoryWithSchema[VersionedTestItemData]
{
	// ATTRIBUTES	--------------------
	
	override lazy val schema = 
		ModelDeclaration(Vector(PropertyDeclaration("name", StringType, isOptional = true), 
			PropertyDeclaration("created", InstantType, isOptional = true), 
			PropertyDeclaration("deprecatedAfter", InstantType, Single("deprecated_after"), 
			isOptional = true)))
	
	
	// IMPLEMENTED	--------------------
	
	override protected def fromValidatedModel(valid: Model) = 
		VersionedTestItemData(valid("name").getString, valid("created").getInstant, 
			valid("deprecatedAfter").instant)
}

/**
  * @param name            Name of this test item
  * @param created         Time when this versioned test item was added to the database
  * @param deprecatedAfter Time when this versioned test item became deprecated. None while this 
  *                        versioned test item is still valid.
  */
case class VersionedTestItemData(name: String = "", created: Instant = Now, 
	deprecatedAfter: Option[Instant] = None) 
	extends VersionedTestItemFactory[VersionedTestItemData] with ModelConvertible
{
	// COMPUTED	--------------------
	
	/**
	  * Whether this versioned test item has already been deprecated
	  */
	def isDeprecated = deprecatedAfter.isDefined
	
	/**
	  * Whether this versioned test item is still valid (not deprecated)
	  */
	def isValid = !isDeprecated
	
	
	// IMPLEMENTED	--------------------
	
	override def toModel = 
		Model(Vector("name" -> name, "created" -> created, "deprecatedAfter" -> deprecatedAfter))
	
	override def withCreated(created: Instant) = copy(created = created)
	
	override def withDeprecatedAfter(deprecatedAfter: Instant) = copy(deprecatedAfter = Some(deprecatedAfter))
	
	override def withName(name: String) = copy(name = name)
}

