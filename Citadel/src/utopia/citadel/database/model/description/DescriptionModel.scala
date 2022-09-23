package utopia.citadel.database.model.description

import java.time.Instant
import utopia.citadel.database.factory.description.DescriptionFactory
import utopia.flow.collection.value.typeless.Value
import utopia.flow.generic.ValueConversions._
import utopia.metropolis.model.partial.description.DescriptionData
import utopia.metropolis.model.stored.description.Description
import utopia.vault.model.immutable.StorableWithFactory
import utopia.vault.nosql.storable.DataInserter
import utopia.vault.nosql.storable.deprecation.DeprecatableAfter

/**
  * Used for constructing DescriptionModel instances and for inserting Descriptions to the database
  * @author Mikko Hilpinen
  * @since 2021-10-23
  */
object DescriptionModel 
	extends DataInserter[DescriptionModel, Description, DescriptionData] 
		with DeprecatableAfter[DescriptionModel]
{
	// ATTRIBUTES	--------------------
	
	/**
	  * Name of the property that contains Description roleId
	  */
	val roleIdAttName = "roleId"
	
	/**
	  * Name of the property that contains Description languageId
	  */
	val languageIdAttName = "languageId"
	
	/**
	  * Name of the property that contains Description text
	  */
	val textAttName = "text"
	
	/**
	  * Name of the property that contains Description authorId
	  */
	val authorIdAttName = "authorId"
	
	/**
	  * Name of the property that contains Description created
	  */
	val createdAttName = "created"
	
	/**
	  * Name of the property that contains Description deprecatedAfter
	  */
	val deprecatedAfterAttName = "deprecatedAfter"
	
	
	// COMPUTED	--------------------
	
	/**
	  * Column that contains Description roleId
	  */
	def roleIdColumn = table(roleIdAttName)
	
	/**
	  * Column that contains Description languageId
	  */
	def languageIdColumn = table(languageIdAttName)
	
	/**
	  * Column that contains Description text
	  */
	def textColumn = table(textAttName)
	
	/**
	  * Column that contains Description authorId
	  */
	def authorIdColumn = table(authorIdAttName)
	
	/**
	  * Column that contains Description created
	  */
	def createdColumn = table(createdAttName)
	
	/**
	  * Column that contains Description deprecatedAfter
	  */
	def deprecatedAfterColumn = table(deprecatedAfterAttName)
	
	/**
	  * The factory object used by this model type
	  */
	def factory = DescriptionFactory
	
	
	// IMPLEMENTED	--------------------
	
	override def table = factory.table
	
	override def apply(data: DescriptionData) = 
		apply(None, Some(data.roleId), Some(data.languageId), Some(data.text), data.authorId, 
			Some(data.created), data.deprecatedAfter)
	
	override def complete(id: Value, data: DescriptionData) = Description(id.getInt, data)
	
	
	// OTHER	--------------------
	
	/**
	  * @param authorId Id of the user who wrote this description (if known and applicable)
	  * @return A model containing only the specified authorId
	  */
	def withAuthorId(authorId: Int) = apply(authorId = Some(authorId))
	
	/**
	  * @param created Time when this description was written
	  * @return A model containing only the specified created
	  */
	def withCreated(created: Instant) = apply(created = Some(created))
	
	/**
	  * @param deprecatedAfter Time when this description was removed or replaced with a new version
	  * @return A model containing only the specified deprecatedAfter
	  */
	def withDeprecatedAfter(deprecatedAfter: Instant) = apply(deprecatedAfter = Some(deprecatedAfter))
	
	/**
	  * @param id A Description id
	  * @return A model with that id
	  */
	def withId(id: Int) = apply(Some(id))
	
	/**
	  * @param languageId Id of the language this description is written in
	  * @return A model containing only the specified languageId
	  */
	def withLanguageId(languageId: Int) = apply(languageId = Some(languageId))
	
	/**
	  * @param roleId Id of the role of this description
	  * @return A model containing only the specified roleId
	  */
	def withRoleId(roleId: Int) = apply(roleId = Some(roleId))
	
	/**
	  * @param text This description as text / written description
	  * @return A model containing only the specified text
	  */
	def withText(text: String) = apply(text = Some(text))
}

/**
  * Used for interacting with Descriptions in the database
  * @param id Description database id
  * @param roleId Id of the role of this description
  * @param languageId Id of the language this description is written in
  * @param text This description as text / written description
  * @param authorId Id of the user who wrote this description (if known and applicable)
  * @param created Time when this description was written
  * @param deprecatedAfter Time when this description was removed or replaced with a new version
  * @author Mikko Hilpinen
  * @since 2021-10-23
  */
case class DescriptionModel(id: Option[Int] = None, roleId: Option[Int] = None, 
	languageId: Option[Int] = None, text: Option[String] = None, authorId: Option[Int] = None, 
	created: Option[Instant] = None, deprecatedAfter: Option[Instant] = None) 
	extends StorableWithFactory[Description]
{
	// IMPLEMENTED	--------------------
	
	override def factory = DescriptionModel.factory
	
	override def valueProperties = 
	{
		import DescriptionModel._
		Vector("id" -> id, roleIdAttName -> roleId, languageIdAttName -> languageId, textAttName -> text, 
			authorIdAttName -> authorId, createdAttName -> created, deprecatedAfterAttName -> deprecatedAfter)
	}
	
	
	// OTHER	--------------------
	
	/**
	  * @param authorId A new authorId
	  * @return A new copy of this model with the specified authorId
	  */
	def withAuthorId(authorId: Int) = copy(authorId = Some(authorId))
	
	/**
	  * @param created A new created
	  * @return A new copy of this model with the specified created
	  */
	def withCreated(created: Instant) = copy(created = Some(created))
	
	/**
	  * @param deprecatedAfter A new deprecatedAfter
	  * @return A new copy of this model with the specified deprecatedAfter
	  */
	def withDeprecatedAfter(deprecatedAfter: Instant) = copy(deprecatedAfter = Some(deprecatedAfter))
	
	/**
	  * @param languageId A new languageId
	  * @return A new copy of this model with the specified languageId
	  */
	def withLanguageId(languageId: Int) = copy(languageId = Some(languageId))
	
	/**
	  * @param roleId A new roleId
	  * @return A new copy of this model with the specified roleId
	  */
	def withRoleId(roleId: Int) = copy(roleId = Some(roleId))
	
	/**
	  * @param text A new text
	  * @return A new copy of this model with the specified text
	  */
	def withText(text: String) = copy(text = Some(text))
}

