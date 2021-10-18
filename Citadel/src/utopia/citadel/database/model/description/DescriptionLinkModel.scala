package utopia.citadel.database.model.description

import java.time.Instant
import utopia.citadel.database.Tables
import utopia.citadel.database.factory.description.DescriptionLinkFactory
import utopia.vault.nosql.storable.deprecation.DeprecatableAfter
import utopia.flow.generic.ValueConversions._
import utopia.vault.model.immutable.StorableWithFactory

object DescriptionLinkModel
{
	/**
	  * Name of the property that contains description link deprecation timestamp
	  */
	def deprecationAttName = DeprecatableAfter.deprecationAttName
	
	/**
	  * Description role links model factory
	  */
	lazy val descriptionRole = DescriptionLinkModelFactory(Tables.descriptionRoleDescription, "roleId")
	/**
	  * Device description links model factory
	  */
	lazy val device = DescriptionLinkModelFactory(Tables.deviceDescription, "deviceId")
	/**
	  * Organization description links model factory
	  */
	lazy val organization = DescriptionLinkModelFactory(Tables.organizationDescription, "organizationId")
	/**
	  * Role description links model factory
	  */
	lazy val userRole = DescriptionLinkModelFactory(Tables.roleDescription, "roleId")
	/**
	  * Task description links model factory
	  */
	lazy val task = DescriptionLinkModelFactory(Tables.taskDescription, "taskId")
	/**
	  * Language description links model factory
	  */
	lazy val language = DescriptionLinkModelFactory(Tables.languageDescription, "languageId")
	/**
	  * Language familiarity description link models factory
	  */
	lazy val languageFamiliarity = DescriptionLinkModelFactory(Tables.languageFamiliarityDescription, "familiarityId")
}

/**
  * Used for interacting with links between devices and their descriptions
  * @author Mikko Hilpinen
  * @since 2.5.2020, v1.0
  */
trait DescriptionLinkModel[+E, +F <: DescriptionLinkFactory[E]] extends StorableWithFactory[E]
{
	// ABSTRACT	------------------------------
	
	override def factory: F
	
	/**
	  * @return Description link id (optional)
	  */
	def id: Option[Int]
	/**
	  * @return Description target id (optional)
	  */
	def targetId: Option[Int]
	/**
	  * @return Description id (optional)
	  */
	def descriptionId: Option[Int]
	/**
	  * @return Creation time of this link
	  */
	def created: Option[Instant]
	/**
	  * @return Description deprecation time (optional)
	  */
	def deprecatedAfter: Option[Instant]
	
	/*
	  * Creates a copy of this model, altering some properties
	  * @param id New link id (default = current)
	  * @param targetId New target item id (default = current)
	  * @param descriptionId New description id (default = current)
	  * @param created New creation time (default = current)
	  * @param deprecatedAfter New deprecation time (default = current)
	  * @return A modified copy of this model
	  */
	/*
	def makeCopy(id: Option[Int] = id, targetId: Option[Int] = targetId, descriptionId: Option[Int] = descriptionId,
	             created: Option[Instant] = created, deprecatedAfter: Option[Instant] = deprecatedAfter): Repr*/
	
	
	// COMPUTED ------------------------------
	
	/*
	  * @return A copy of this model that has just been marked as deprecated
	  */
	// def nowDeprecated = makeCopy(deprecatedAfter = Some(Now))
	
	
	// IMPLEMENTED	--------------------------
	
	override def valueProperties = Vector("id" -> id, factory.model.targetIdAttName -> targetId,
		"descriptionId" -> descriptionId, "created" -> created, "deprecatedAfter" -> deprecatedAfter)
}