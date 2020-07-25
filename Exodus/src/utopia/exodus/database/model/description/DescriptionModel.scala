package utopia.exodus.database.model.description

import utopia.exodus.database.factory.description.DescriptionFactory
import utopia.flow.generic.ValueConversions._
import utopia.metropolis.model.partial.description.DescriptionData
import utopia.metropolis.model.stored.description.Description
import utopia.vault.database.Connection
import utopia.vault.model.immutable.StorableWithFactory

object DescriptionModel
{
	// ATTRIBUTES	--------------------------------
	
	/**
	  * Name of the attribute that contains description role id
	  */
	val descriptionRoleIdAttName = "roleId"
	
	
	// COMPUTED	-------------------------------------
	
	/**
	  * @return Factory associated with this model
	  */
	def factory = DescriptionFactory
	
	/**
	  * @return Table used by this model
	  */
	def table = factory.table
	
	/**
	  * @return Column that contains description role id
	  */
	def descriptionRoleIdColumn = table(descriptionRoleIdAttName)
	
	
	// OTHER	-------------------------------------
	
	/**
	  * @param roleId Id if the Description's role
	  * @return A model with only description role set
	  */
	def withRoleId(roleId: Int) = apply(roleId = Some(roleId))
	
	/**
	  * @param languageId Description language id
	  * @return A model with only language id set
	  */
	def withLanguageId(languageId: Int) = apply(languageId = Some(languageId))
	
	/**
	  * Inserts a new description to the DB
	  * @param data Data to insert
	  * @param connection DB Connection (implicit)
	  * @return Newly inserted description
	  */
	def insert(data: DescriptionData)(implicit connection: Connection) =
	{
		val newId = apply(None, Some(data.roleId), Some(data.languageId), Some(data.text), data.authorId).insert().getInt
		Description(newId, data)
	}
}

/**
  * Used for interacting with descriptions in DB
  * @author Mikko Hilpinen
  * @since 2.5.2020, v1
  */
case class DescriptionModel(id: Option[Int] = None, roleId: Option[Int] = None, languageId: Option[Int] = None,
							text: Option[String] = None, authorId: Option[Int] = None)
	extends StorableWithFactory[Description]
{
	import DescriptionModel._
	
	// IMPLEMENTED	--------------------------------
	
	override def factory = DescriptionFactory
	
	override def valueProperties = Vector("id" -> id, descriptionRoleIdAttName -> roleId,
		"languageId" -> languageId, "text" -> text, "authorId" -> authorId)
	
	
	// OTHER	------------------------------------
	
	/**
	  * @param languageId Id of description language
	  * @return A copy of this model with specified language
	  */
	def withLanguageId(languageId: Int) = copy(languageId = Some(languageId))
}
