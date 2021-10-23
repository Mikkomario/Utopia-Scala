package utopia.citadel.database.access.single.user

import java.time.Instant
import utopia.citadel.database.factory.user.UserLanguageFactory
import utopia.citadel.database.model.user.UserLanguageModel
import utopia.flow.datastructure.immutable.Value
import utopia.flow.generic.ValueConversions._
import utopia.metropolis.model.stored.user.UserLanguage
import utopia.vault.database.Connection
import utopia.vault.nosql.access.single.model.SingleRowModelAccess
import utopia.vault.nosql.access.template.model.DistinctModelAccess
import utopia.vault.nosql.template.Indexed

/**
  * A common trait for access points that return individual and distinct UserLanguages.
  * @author Mikko Hilpinen
  * @since 2021-10-23
  */
trait UniqueUserLanguageAccess 
	extends SingleRowModelAccess[UserLanguage] 
		with DistinctModelAccess[UserLanguage, Option[UserLanguage], Value] with Indexed
{
	// COMPUTED	--------------------
	
	/**
	  * Id of the user who's being described. None if no instance (or value) was found.
	  */
	def userId(implicit connection: Connection) = pullColumn(model.userIdColumn).int
	
	/**
	  * Id of the language known to the user. None if no instance (or value) was found.
	  */
	def languageId(implicit connection: Connection) = pullColumn(model.languageIdColumn).int
	
	/**
	  * Id of the user's familiarity level in the referenced language. None if no instance (or value) was found.
	  */
	def familiarityId(implicit connection: Connection) = pullColumn(model.familiarityIdColumn).int
	
	/**
	  * Time when this UserLanguage was first created. None if no instance (or value) was found.
	  */
	def created(implicit connection: Connection) = pullColumn(model.createdColumn).instant
	
	def id(implicit connection: Connection) = pullColumn(index).int
	
	/**
	  * Factory used for constructing database the interaction models
	  */
	protected def model = UserLanguageModel
	
	
	// IMPLEMENTED	--------------------
	
	override def factory = UserLanguageFactory
	
	
	// OTHER	--------------------
	
	/**
	  * Updates the created of the targeted UserLanguage instance(s)
	  * @param newCreated A new created to assign
	  * @return Whether any UserLanguage instance was affected
	  */
	def created_=(newCreated: Instant)(implicit connection: Connection) = 
		putColumn(model.createdColumn, newCreated)
	
	/**
	  * Updates the familiarityId of the targeted UserLanguage instance(s)
	  * @param newFamiliarityId A new familiarityId to assign
	  * @return Whether any UserLanguage instance was affected
	  */
	def familiarityId_=(newFamiliarityId: Int)(implicit connection: Connection) = 
		putColumn(model.familiarityIdColumn, newFamiliarityId)
	
	/**
	  * Updates the languageId of the targeted UserLanguage instance(s)
	  * @param newLanguageId A new languageId to assign
	  * @return Whether any UserLanguage instance was affected
	  */
	def languageId_=(newLanguageId: Int)(implicit connection: Connection) = 
		putColumn(model.languageIdColumn, newLanguageId)
	
	/**
	  * Updates the userId of the targeted UserLanguage instance(s)
	  * @param newUserId A new userId to assign
	  * @return Whether any UserLanguage instance was affected
	  */
	def userId_=(newUserId: Int)(implicit connection: Connection) = putColumn(model.userIdColumn, newUserId)
}

