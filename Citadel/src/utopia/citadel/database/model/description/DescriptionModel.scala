package utopia.citadel.database.model.description

import utopia.citadel.database.factory.description.DescriptionFactory
import utopia.flow.datastructure.immutable.Value
import utopia.flow.generic.ValueConversions._
import utopia.metropolis.model.partial.description.DescriptionData
import utopia.metropolis.model.stored.description.Description
import utopia.vault.model.immutable.StorableWithFactory
import utopia.vault.model.template.DataInserter

object DescriptionModel extends DataInserter[DescriptionModel, Description, DescriptionData]
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
	  * @return Column that contains description role id
	  */
	def descriptionRoleIdColumn = table(descriptionRoleIdAttName)
	
	
	// IMPLEMENTED  ---------------------------------
	
	/**
	  * @return Table used by this model
	  */
	override def table = factory.table
	
	override def apply(data: DescriptionData) =
		apply(None, Some(data.roleId), Some(data.languageId), Some(data.text), data.authorId)
	
	override protected def complete(id: Value, data: DescriptionData) = Description(id.getInt, data)
	
	
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
}

/**
  * Used for interacting with descriptions in DB
  * @author Mikko Hilpinen
  * @since 2.5.2020, v1.0
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
