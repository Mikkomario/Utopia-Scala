package utopia.citadel.database.access.many.user

import utopia.citadel.database.model.user.UserLanguageLinkModel
import utopia.flow.generic.casting.ValueConversions._
import utopia.vault.database.Connection
import utopia.vault.nosql.access.many.model.{ManyModelAccess, ManyRowModelAccess}
import utopia.vault.nosql.template.Indexed
import utopia.vault.nosql.view.FilterableView
import utopia.vault.sql.Condition
import utopia.vault.sql.SqlExtensions._

import java.time.Instant


/**
  * A common trait for access points which target multiple user languages links or similar instances at a time
  * @author Mikko Hilpinen
  * @since 2021-10-23
  */
trait ManyUserLanguageLinksAccessLike[+A, +Repr <: ManyModelAccess[A]]
	extends ManyRowModelAccess[A] with Indexed with FilterableView[Repr]
{
	// ABSTRACT --------------------
	
	protected def _filter(condition: Condition): Repr
	
	
	// COMPUTED	--------------------
	
	/**
	  * userIds of the accessible UserLanguages
	  */
	def userIds(implicit connection: Connection) = pullColumn(model.userIdColumn)
		.flatMap { value => value.int }
	/**
	  * languageIds of the accessible UserLanguages
	  */
	def languageIds(implicit connection: Connection) =
		pullColumn(model.languageIdColumn).flatMap { value => value.int }
	/**
	  * familiarityIds of the accessible UserLanguages
	  */
	def familiarityIds(implicit connection: Connection) =
		pullColumn(model.familiarityIdColumn).flatMap { value => value.int }
	/**
	  * creationTimes of the accessible UserLanguages
	  */
	def creationTimes(implicit connection: Connection) =
		pullColumn(model.createdColumn).flatMap { value => value.instant }
	
	def ids(implicit connection: Connection) = pullColumn(index).flatMap { id => id.int }
	
	/**
	  * Factory used for constructing database the interaction models
	  */
	protected def model = UserLanguageLinkModel
	
	
	// IMPLEMENTED	--------------------
	
	override def filter(additionalCondition: Condition) = _filter(additionalCondition)
	
	
	// OTHER	--------------------
	
	/**
	  * @param userId Id of the linked user
	  * @return An access point to language links concerning that user
	  */
	def withUserId(userId: Int) = filter(model.withUserId(userId).toCondition)
	/**
	  * @param languageId Id of linked language
	  * @return An access point to links to that language
	  */
	def withLanguageId(languageId: Int) = filter(model.withLanguageId(languageId).toCondition)
	
	/**
	  * @param languageIds Ids of targeted languages
	  * @return An access point to language links concerning specified languages only
	  *         (but not necessarily including them all)
	  */
	def withAnyOfLanguages(languageIds: Iterable[Int]) = filter(model.languageIdColumn in languageIds)
	
	/**
	  * Updates the created of the targeted UserLanguage instance(s)
	  * @param newCreated A new created to assign
	  * @return Whether any UserLanguage instance was affected
	  */
	def creationTimes_=(newCreated: Instant)(implicit connection: Connection) = 
		putColumn(model.createdColumn, newCreated)
	/**
	  * Updates the familiarityId of the targeted UserLanguage instance(s)
	  * @param newFamiliarityId A new familiarityId to assign
	  * @return Whether any UserLanguage instance was affected
	  */
	def familiarityIds_=(newFamiliarityId: Int)(implicit connection: Connection) = 
		putColumn(model.familiarityIdColumn, newFamiliarityId)
	/**
	  * Updates the languageId of the targeted UserLanguage instance(s)
	  * @param newLanguageId A new languageId to assign
	  * @return Whether any UserLanguage instance was affected
	  */
	def languageIds_=(newLanguageId: Int)(implicit connection: Connection) = 
		putColumn(model.languageIdColumn, newLanguageId)
	/**
	  * Updates the userId of the targeted UserLanguage instance(s)
	  * @param newUserId A new userId to assign
	  * @return Whether any UserLanguage instance was affected
	  */
	def userIds_=(newUserId: Int)(implicit connection: Connection) = putColumn(model.userIdColumn, newUserId)
}

