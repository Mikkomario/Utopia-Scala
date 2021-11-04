package utopia.ambassador.database.access.single.process

import java.time.Instant
import utopia.ambassador.database.factory.process.AuthRedirectResultFactory
import utopia.ambassador.database.model.process.AuthRedirectResultModel
import utopia.ambassador.model.stored.process.AuthRedirectResult
import utopia.flow.datastructure.immutable.Value
import utopia.flow.generic.ValueConversions._
import utopia.vault.database.Connection
import utopia.vault.nosql.access.single.model.SingleRowModelAccess
import utopia.vault.nosql.access.template.model.DistinctModelAccess
import utopia.vault.nosql.template.Indexed

/**
  * A common trait for access points that return individual and distinct AuthRedirectResults.
  * @author Mikko Hilpinen
  * @since 2021-10-26
  */
trait UniqueAuthRedirectResultAccess 
	extends SingleRowModelAccess[AuthRedirectResult] 
		with DistinctModelAccess[AuthRedirectResult, Option[AuthRedirectResult], Value] with Indexed
{
	// COMPUTED	--------------------
	
	/**
	  * Id of the redirection event this result completes. None if no instance (or value) was found.
	  */
	def redirectId(implicit connection: Connection) = pullColumn(model.redirectIdColumn).int
	
	/**
	  * Whether an authentication code was included in the request (implies success). None if no instance (or value) was found.
	  */
	def didReceiveCode(implicit connection: Connection) = pullColumn(model.didReceiveCodeColumn).boolean
	
	/**
	  * Whether authentication tokens were successfully acquired. None if no instance (or value) was found.
	  */
	def didReceiveToken(implicit connection: Connection) = pullColumn(model.didReceiveTokenColumn).boolean
	
	/**
	  * Time when this AuthRedirectResult was first created. None if no instance (or value) was found.
	  */
	def created(implicit connection: Connection) = pullColumn(model.createdColumn).instant
	
	def id(implicit connection: Connection) = pullColumn(index).int
	
	/**
	  * Factory used for constructing database the interaction models
	  */
	protected def model = AuthRedirectResultModel
	
	
	// IMPLEMENTED	--------------------
	
	override def factory = AuthRedirectResultFactory
	
	
	// OTHER	--------------------
	
	/**
	  * Updates the created of the targeted AuthRedirectResult instance(s)
	  * @param newCreated A new created to assign
	  * @return Whether any AuthRedirectResult instance was affected
	  */
	def created_=(newCreated: Instant)(implicit connection: Connection) = 
		putColumn(model.createdColumn, newCreated)
	
	/**
	  * Updates the didReceiveCode of the targeted AuthRedirectResult instance(s)
	  * @param newDidReceiveCode A new didReceiveCode to assign
	  * @return Whether any AuthRedirectResult instance was affected
	  */
	def didReceiveCode_=(newDidReceiveCode: Boolean)(implicit connection: Connection) = 
		putColumn(model.didReceiveCodeColumn, newDidReceiveCode)
	
	/**
	  * Updates the didReceiveToken of the targeted AuthRedirectResult instance(s)
	  * @param newDidReceiveToken A new didReceiveToken to assign
	  * @return Whether any AuthRedirectResult instance was affected
	  */
	def didReceiveToken_=(newDidReceiveToken: Boolean)(implicit connection: Connection) = 
		putColumn(model.didReceiveTokenColumn, newDidReceiveToken)
	
	/**
	  * Updates the redirectId of the targeted AuthRedirectResult instance(s)
	  * @param newRedirectId A new redirectId to assign
	  * @return Whether any AuthRedirectResult instance was affected
	  */
	def redirectId_=(newRedirectId: Int)(implicit connection: Connection) = 
		putColumn(model.redirectIdColumn, newRedirectId)
}

