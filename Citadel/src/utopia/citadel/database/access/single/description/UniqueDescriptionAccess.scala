package utopia.citadel.database.access.single.description

import java.time.Instant
import utopia.citadel.database.factory.description.DescriptionFactory
import utopia.citadel.database.model.description.DescriptionModel
import utopia.flow.collection.value.typeless.Value
import utopia.flow.generic.ValueConversions._
import utopia.metropolis.model.stored.description.Description
import utopia.vault.database.Connection
import utopia.vault.nosql.access.single.model.SingleRowModelAccess
import utopia.vault.nosql.access.template.model.DistinctModelAccess
import utopia.vault.nosql.template.Indexed

/**
  * A common trait for access points that return individual and distinct Descriptions.
  * @author Mikko Hilpinen
  * @since 2021-10-23
  */
trait UniqueDescriptionAccess 
	extends SingleRowModelAccess[Description] 
		with DistinctModelAccess[Description, Option[Description], Value] with Indexed
{
	// COMPUTED	--------------------
	
	/**
	  * Id of the role of this description. None if no instance (or value) was found.
	  */
	def roleId(implicit connection: Connection) = pullColumn(model.roleIdColumn).int
	
	/**
	  * Id of the language this description is written in. None if no instance (or value) was found.
	  */
	def languageId(implicit connection: Connection) = pullColumn(model.languageIdColumn).int
	
	/**
	  * This description as text / written description. None if no instance (or value) was found.
	  */
	def text(implicit connection: Connection) = pullColumn(model.textColumn).string
	
	/**
	  * Id of the user who wrote this description (if known and applicable). None if no instance (or value) was found.
	  */
	def authorId(implicit connection: Connection) = pullColumn(model.authorIdColumn).int
	
	/**
	  * Time when this description was written. None if no instance (or value) was found.
	  */
	def created(implicit connection: Connection) = pullColumn(model.createdColumn).instant
	
	/**
	  * Time when this description was removed or replaced 
		with a new version. None if no instance (or value) was found.
	  */
	def deprecatedAfter(implicit connection: Connection) = pullColumn(model.deprecatedAfterColumn).instant
	
	def id(implicit connection: Connection) = pullColumn(index).int
	
	/**
	  * Factory used for constructing database the interaction models
	  */
	protected def model = DescriptionModel
	
	
	// IMPLEMENTED	--------------------
	
	override def factory = DescriptionFactory
	
	
	// OTHER	--------------------
	
	/**
	  * Updates the authorId of the targeted Description instance(s)
	  * @param newAuthorId A new authorId to assign
	  * @return Whether any Description instance was affected
	  */
	def authorId_=(newAuthorId: Int)(implicit connection: Connection) = 
		putColumn(model.authorIdColumn, newAuthorId)
	
	/**
	  * Updates the created of the targeted Description instance(s)
	  * @param newCreated A new created to assign
	  * @return Whether any Description instance was affected
	  */
	def created_=(newCreated: Instant)(implicit connection: Connection) = 
		putColumn(model.createdColumn, newCreated)
	
	/**
	  * Updates the deprecatedAfter of the targeted Description instance(s)
	  * @param newDeprecatedAfter A new deprecatedAfter to assign
	  * @return Whether any Description instance was affected
	  */
	def deprecatedAfter_=(newDeprecatedAfter: Instant)(implicit connection: Connection) = 
		putColumn(model.deprecatedAfterColumn, newDeprecatedAfter)
	
	/**
	  * Updates the languageId of the targeted Description instance(s)
	  * @param newLanguageId A new languageId to assign
	  * @return Whether any Description instance was affected
	  */
	def languageId_=(newLanguageId: Int)(implicit connection: Connection) = 
		putColumn(model.languageIdColumn, newLanguageId)
	
	/**
	  * Updates the roleId of the targeted Description instance(s)
	  * @param newRoleId A new roleId to assign
	  * @return Whether any Description instance was affected
	  */
	def roleId_=(newRoleId: Int)(implicit connection: Connection) = putColumn(model.roleIdColumn, newRoleId)
	
	/**
	  * Updates the text of the targeted Description instance(s)
	  * @param newText A new text to assign
	  * @return Whether any Description instance was affected
	  */
	def text_=(newText: String)(implicit connection: Connection) = putColumn(model.textColumn, newText)
}

