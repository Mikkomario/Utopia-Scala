package utopia.exodus.database.model.description

import java.time.Instant

import utopia.exodus.database.Tables
import utopia.exodus.database.factory.description.DescriptionLinkFactory
import utopia.flow.generic.ValueConversions._
import utopia.vault.model.immutable.StorableWithFactory

/**
  * Used for interacting with links between devices and their descriptions
  * @author Mikko Hilpinen
  * @since 2.5.2020, v1
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
	
	
	// IMPLEMENTED	--------------------------
	
	override def valueProperties = Vector("id" -> id, factory.modelFactory.targetIdAttName -> targetId,
		"descriptionId" -> descriptionId, "created" -> created, "deprecatedAfter" -> deprecatedAfter)
}

object DescriptionLinkModel
{
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