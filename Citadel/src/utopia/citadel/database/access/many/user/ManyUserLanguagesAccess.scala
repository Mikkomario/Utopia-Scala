package utopia.citadel.database.access.many.user

import java.time.Instant
import utopia.citadel.database.factory.user.UserLanguageFactory
import utopia.citadel.database.model.user.UserLanguageModel
import utopia.flow.generic.ValueConversions._
import utopia.metropolis.model.stored.user.UserLanguage
import utopia.vault.database.Connection
import utopia.vault.nosql.access.many.model.ManyRowModelAccess
import utopia.vault.nosql.template.Indexed
import utopia.vault.nosql.view.SubView
import utopia.vault.sql.Condition

object ManyUserLanguagesAccess
{
	// NESTED	--------------------
	
	private class ManyUserLanguagesSubView(override val parent: ManyRowModelAccess[UserLanguage], 
		override val filterCondition: Condition) 
		extends ManyUserLanguagesAccess with SubView
}

/**
  * A common trait for access points which target multiple UserLanguages at a time
  * @author Mikko Hilpinen
  * @since 2021-10-23
  */
trait ManyUserLanguagesAccess extends ManyRowModelAccess[UserLanguage] with Indexed
{
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
	protected def model = UserLanguageModel
	
	
	// IMPLEMENTED	--------------------
	
	override def factory = UserLanguageFactory
	
	override protected def defaultOrdering = None
	
	override def filter(additionalCondition: Condition): ManyUserLanguagesAccess = 
		new ManyUserLanguagesAccess.ManyUserLanguagesSubView(this, additionalCondition)
	
	
	// OTHER	--------------------
	
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

