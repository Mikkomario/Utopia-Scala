package utopia.ambassador.database.access.many.process

import java.time.Instant
import utopia.ambassador.database.factory.process.AuthRedirectResultFactory
import utopia.ambassador.database.model.process.AuthRedirectResultModel
import utopia.ambassador.model.stored.process.AuthRedirectResult
import utopia.flow.generic.ValueConversions._
import utopia.vault.database.Connection
import utopia.vault.nosql.access.many.model.ManyRowModelAccess
import utopia.vault.nosql.template.Indexed
import utopia.vault.nosql.view.{FilterableView, SubView}
import utopia.vault.sql.Condition

object ManyAuthRedirectResultsAccess
{
	// NESTED	--------------------
	
	private class ManyAuthRedirectResultsSubView(override val parent: ManyRowModelAccess[AuthRedirectResult], 
		override val filterCondition: Condition) 
		extends ManyAuthRedirectResultsAccess with SubView
}

/**
  * A common trait for access points which target multiple AuthRedirectResults at a time
  * @author Mikko Hilpinen
  * @since 2021-10-26
  */
trait ManyAuthRedirectResultsAccess
	extends ManyRowModelAccess[AuthRedirectResult] with Indexed with FilterableView[ManyAuthRedirectResultsAccess]
{
	// COMPUTED	--------------------
	
	/**
	  * redirectIds of the accessible AuthRedirectResults
	  */
	def redirectIds(implicit connection: Connection) = 
		pullColumn(model.redirectIdColumn).flatMap { value => value.int }
	
	/**
	  * didReceiveCodes of the accessible AuthRedirectResults
	  */
	def didReceiveCodes(implicit connection: Connection) = 
		pullColumn(model.didReceiveCodeColumn).flatMap { value => value.boolean }
	
	/**
	  * didReceiveTokens of the accessible AuthRedirectResults
	  */
	def didReceiveTokens(implicit connection: Connection) = 
		pullColumn(model.didReceiveTokenColumn).flatMap { value => value.boolean }
	
	/**
	  * creationTimes of the accessible AuthRedirectResults
	  */
	def creationTimes(implicit connection: Connection) = 
		pullColumn(model.createdColumn).flatMap { value => value.instant }
	
	def ids(implicit connection: Connection) = pullColumn(index).flatMap { id => id.int }
	
	/**
	  * Factory used for constructing database the interaction models
	  */
	protected def model = AuthRedirectResultModel
	
	
	// IMPLEMENTED	--------------------
	
	override def factory = AuthRedirectResultFactory
	
	override def filter(additionalCondition: Condition): ManyAuthRedirectResultsAccess = 
		new ManyAuthRedirectResultsAccess.ManyAuthRedirectResultsSubView(this, additionalCondition)
	
	
	// OTHER	--------------------
	
	/**
	  * Updates the created of the targeted AuthRedirectResult instance(s)
	  * @param newCreated A new created to assign
	  * @return Whether any AuthRedirectResult instance was affected
	  */
	def creationTimes_=(newCreated: Instant)(implicit connection: Connection) = 
		putColumn(model.createdColumn, newCreated)
	
	/**
	  * Updates the didReceiveCode of the targeted AuthRedirectResult instance(s)
	  * @param newDidReceiveCode A new didReceiveCode to assign
	  * @return Whether any AuthRedirectResult instance was affected
	  */
	def didReceiveCodes_=(newDidReceiveCode: Boolean)(implicit connection: Connection) = 
		putColumn(model.didReceiveCodeColumn, newDidReceiveCode)
	
	/**
	  * Updates the didReceiveToken of the targeted AuthRedirectResult instance(s)
	  * @param newDidReceiveToken A new didReceiveToken to assign
	  * @return Whether any AuthRedirectResult instance was affected
	  */
	def didReceiveTokens_=(newDidReceiveToken: Boolean)(implicit connection: Connection) = 
		putColumn(model.didReceiveTokenColumn, newDidReceiveToken)
	
	/**
	  * Updates the redirectId of the targeted AuthRedirectResult instance(s)
	  * @param newRedirectId A new redirectId to assign
	  * @return Whether any AuthRedirectResult instance was affected
	  */
	def redirectIds_=(newRedirectId: Int)(implicit connection: Connection) = 
		putColumn(model.redirectIdColumn, newRedirectId)
}

