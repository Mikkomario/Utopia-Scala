package utopia.citadel.database.access.many.description

import utopia.citadel.database.factory.description.DescriptionFactory
import utopia.citadel.database.model.description.DescriptionModel
import utopia.flow.generic.casting.ValueConversions._
import utopia.metropolis.model.stored.description.Description
import utopia.vault.database.Connection
import utopia.vault.nosql.access.many.model.ManyRowModelAccess
import utopia.vault.nosql.template.Indexed
import utopia.vault.nosql.view.FilterableView
import utopia.vault.sql.Condition

import java.time.Instant

object ManyDescriptionsAccess
{
	// OTHER	--------------------
	
	def apply(condition: Condition): ManyDescriptionsAccess = _Access(Some(condition))
	
	
	// NESTED	--------------------
	
	private case class _Access(accessCondition: Option[Condition]) extends ManyDescriptionsAccess
}

/**
  * A common trait for access points which target multiple Descriptions at a time
  * @author Mikko Hilpinen
  * @since 23.10.2021
  */
trait ManyDescriptionsAccess 
	extends ManyRowModelAccess[Description] with Indexed with FilterableView[ManyDescriptionsAccess]
{
	// COMPUTED	--------------------
	
	/**
	  * roleIds of the accessible Descriptions
	  */
	def roleIds(implicit connection: Connection) = pullColumn(model.roleIdColumn)
		.flatMap { value => value.int }
	
	/**
	  * languageIds of the accessible Descriptions
	  */
	def languageIds(implicit connection: Connection) = 
		pullColumn(model.languageIdColumn).flatMap { value => value.int }
	
	/**
	  * texts of the accessible Descriptions
	  */
	def texts(implicit connection: Connection) = pullColumn(model.textColumn)
		.flatMap { value => value.string }
	
	/**
	  * authorIds of the accessible Descriptions
	  */
	def authorIds(implicit connection: Connection) = 
		pullColumn(model.authorIdColumn).flatMap { value => value.int }
	
	/**
	  * creationTimes of the accessible Descriptions
	  */
	def creationTimes(implicit connection: Connection) = 
		pullColumn(model.createdColumn).flatMap { value => value.instant }
	
	/**
	  * deprecationTimes of the accessible Descriptions
	  */
	def deprecationTimes(implicit connection: Connection) = 
		pullColumn(model.deprecatedAfterColumn).flatMap { value => value.instant }
	
	def ids(implicit connection: Connection) = pullColumn(index).flatMap { id => id.int }
	
	/**
	  * Factory used for constructing database the interaction models
	  */
	protected def model = DescriptionModel
	
	
	// IMPLEMENTED	--------------------
	
	override def factory = DescriptionFactory
	
	override protected def self = this
	
	override def apply(condition: Condition): ManyDescriptionsAccess = ManyDescriptionsAccess(condition)
	
	
	// OTHER	--------------------
	
	/**
	  * Updates the authorId of the targeted Description instance(s)
	  * @param newAuthorId A new authorId to assign
	  * @return Whether any Description instance was affected
	  */
	def authorIds_=(newAuthorId: Int)(implicit connection: Connection) = 
		putColumn(model.authorIdColumn, newAuthorId)
	
	/**
	  * Updates the created of the targeted Description instance(s)
	  * @param newCreated A new created to assign
	  * @return Whether any Description instance was affected
	  */
	def creationTimes_=(newCreated: Instant)(implicit connection: Connection) = 
		putColumn(model.createdColumn, newCreated)
	
	/**
	  * Updates the deprecatedAfter of the targeted Description instance(s)
	  * @param newDeprecatedAfter A new deprecatedAfter to assign
	  * @return Whether any Description instance was affected
	  */
	def deprecationTimes_=(newDeprecatedAfter: Instant)(implicit connection: Connection) = 
		putColumn(model.deprecatedAfterColumn, newDeprecatedAfter)
	
	/**
	  * Updates the languageId of the targeted Description instance(s)
	  * @param newLanguageId A new languageId to assign
	  * @return Whether any Description instance was affected
	  */
	def languageIds_=(newLanguageId: Int)(implicit connection: Connection) = 
		putColumn(model.languageIdColumn, newLanguageId)
	
	/**
	  * Updates the roleId of the targeted Description instance(s)
	  * @param newRoleId A new roleId to assign
	  * @return Whether any Description instance was affected
	  */
	def roleIds_=(newRoleId: Int)(implicit connection: Connection) = putColumn(model.roleIdColumn, newRoleId)
	
	/**
	  * Updates the text of the targeted Description instance(s)
	  * @param newText A new text to assign
	  * @return Whether any Description instance was affected
	  */
	def texts_=(newText: String)(implicit connection: Connection) = putColumn(model.textColumn, newText)
}

