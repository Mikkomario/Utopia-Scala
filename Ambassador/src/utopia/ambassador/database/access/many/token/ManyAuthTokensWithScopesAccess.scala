package utopia.ambassador.database.access.many.token

import utopia.ambassador.database.factory.token.AuthTokenWithScopesFactory
import utopia.ambassador.database.model.scope.ScopeModel
import utopia.ambassador.model.combined.token.AuthTokenWithScopes
import utopia.flow.generic.casting.ValueConversions._
import utopia.vault.database.Connection
import utopia.vault.nosql.view.ViewFactory
import utopia.vault.sql.Condition

object ManyAuthTokensWithScopesAccess extends ViewFactory[ManyAuthTokensWithScopesAccess]
{
	// IMPLEMENTED	--------------------
	
	/**
	  * @param condition Condition to apply to all requests
	  * @return An access point that applies the specified filter condition (only)
	  */
	override def apply(condition: Condition): ManyAuthTokensWithScopesAccess = 
		new _ManyAuthTokensWithScopesAccess(condition)
	
	
	// NESTED	--------------------
	
	private class _ManyAuthTokensWithScopesAccess(condition: Condition) extends ManyAuthTokensWithScopesAccess
	{
		// IMPLEMENTED	--------------------
		
		override def accessCondition = Some(condition)
	}
}

/**
  * Common trait for access points which return multiple authentication tokens at a time and include
  * scope information
  * @author Mikko Hilpinen
  * @since 27.10.2021, v2.0
  */
trait ManyAuthTokensWithScopesAccess 
	extends ManyAuthTokensAccessLike[AuthTokenWithScopes, ManyAuthTokensWithScopesAccess]
{
	// COMPUTED	--------------------
	
	/**
	  * Scope ids visible from this access point
	  * @param connection Implicit DB Connection
	  */
	def scopeIds(implicit connection: Connection) = pullColumn(scopeModel.index).flatMap { _.int }.toSet
	
	/**
	  * Model used for interacting with scope data in the DB
	  */
	protected def scopeModel = ScopeModel
	
	
	// IMPLEMENTED	--------------------
	
	override def factory = AuthTokenWithScopesFactory
	
	override protected def self = this
	
	override def apply(condition: Condition): ManyAuthTokensWithScopesAccess = 
		ManyAuthTokensWithScopesAccess(condition)
	
	
	// OTHER	--------------------
	
	/**
	  * @param serviceIds Ids of the targeted service
	  * @return An access point to those of these scopes which are linked to one of those services
	  */
	def forAnyOfServices(serviceIds: Iterable[Int]) = filter(scopeModel.serviceIdColumn in serviceIds)
	
	/**
	  * @param serviceId Id of the targeted service
	  * @return An access point to tokens that are used with that service
	  */
	def forServiceWithId(serviceId: Int) = filter(scopeModel.withServiceId(serviceId).toCondition)
}

