package utopia.ambassador.database.access.single.process

import utopia.ambassador.database.factory.process.AuthCompletionRedirectTargetFactory
import utopia.ambassador.database.model.process.AuthCompletionRedirectTargetModel
import utopia.ambassador.model.stored.process.AuthCompletionRedirectTarget
import utopia.flow.datastructure.immutable.Value
import utopia.flow.generic.ValueConversions._
import utopia.vault.database.Connection
import utopia.vault.nosql.access.single.model.SingleRowModelAccess
import utopia.vault.nosql.access.template.model.DistinctModelAccess
import utopia.vault.nosql.template.Indexed

/**
  * A common trait for access points that return individual and distinct AuthCompletionRedirectTargets.
  * @author Mikko Hilpinen
  * @since 2021-10-26
  */
trait UniqueAuthCompletionRedirectTargetAccess 
	extends SingleRowModelAccess[AuthCompletionRedirectTarget] 
		with DistinctModelAccess[AuthCompletionRedirectTarget, Option[AuthCompletionRedirectTarget], Value] 
		with Indexed
{
	// COMPUTED	--------------------
	
	/**
	  * Id of the preparation during which these targets were specified. None if no instance (or value) was found.
	  */
	def preparationId(implicit connection: Connection) = pullColumn(model.preparationIdColumn).int
	
	/**
	  * Url where the user will be redirected. None if no instance (or value) was found.
	  */
	def url(implicit connection: Connection) = pullColumn(model.urlColumn).string
	
	/**
	  * True when only successes are accepted. False when only failures are accepted. None when both are accepted.. None if no instance (or value) was found.
	  */
	def resultStateFilter(implicit connection: Connection) = pullColumn(model.resultStateFilterColumn).boolean
	
	/**
	  * Whether this target is only used for denial of access -cases. None if no instance (or value) was found.
	  */
	def isLimitedToDenials(implicit connection: Connection) = pullColumn(model.isLimitedToDenialsColumn).boolean
	
	def id(implicit connection: Connection) = pullColumn(index).int
	
	/**
	  * Factory used for constructing database the interaction models
	  */
	protected def model = AuthCompletionRedirectTargetModel
	
	
	// IMPLEMENTED	--------------------
	
	override def factory = AuthCompletionRedirectTargetFactory
	
	
	// OTHER	--------------------
	
	/**
	  * Updates the isLimitedToDenials of the targeted AuthCompletionRedirectTarget instance(s)
	  * @param newIsLimitedToDenials A new isLimitedToDenials to assign
	  * @return Whether any AuthCompletionRedirectTarget instance was affected
	  */
	def isLimitedToDenials_=(newIsLimitedToDenials: Boolean)(implicit connection: Connection) = 
		putColumn(model.isLimitedToDenialsColumn, newIsLimitedToDenials)
	
	/**
	  * Updates the preparationId of the targeted AuthCompletionRedirectTarget instance(s)
	  * @param newPreparationId A new preparationId to assign
	  * @return Whether any AuthCompletionRedirectTarget instance was affected
	  */
	def preparationId_=(newPreparationId: Int)(implicit connection: Connection) = 
		putColumn(model.preparationIdColumn, newPreparationId)
	
	/**
	  * Updates the resultStateFilter of the targeted AuthCompletionRedirectTarget instance(s)
	  * @param newResultStateFilter A new resultStateFilter to assign
	  * @return Whether any AuthCompletionRedirectTarget instance was affected
	  */
	def resultStateFilter_=(newResultStateFilter: Boolean)(implicit connection: Connection) = 
		putColumn(model.resultStateFilterColumn, newResultStateFilter)
	
	/**
	  * Updates the url of the targeted AuthCompletionRedirectTarget instance(s)
	  * @param newUrl A new url to assign
	  * @return Whether any AuthCompletionRedirectTarget instance was affected
	  */
	def url_=(newUrl: String)(implicit connection: Connection) = putColumn(model.urlColumn, newUrl)
}

