package utopia.ambassador.database.access.many.token

import utopia.ambassador.database.access.many.token.ManyAuthTokensWithScopesAccess.SubAccess
import utopia.ambassador.database.factory.token.AuthTokenWithScopesFactory
import utopia.ambassador.database.model.scope.ScopeModel
import utopia.ambassador.model.combined.token.AuthTokenWithScopes
import utopia.flow.generic.casting.ValueConversions._
import utopia.vault.database.Connection
import utopia.vault.nosql.access.many.model.ManyModelAccess
import utopia.vault.nosql.view.SubView
import utopia.vault.sql.Condition
import utopia.vault.sql.SqlExtensions._

object ManyAuthTokensWithScopesAccess
{
	private class SubAccess(override val parent: ManyModelAccess[AuthTokenWithScopes],
	                        override val filterCondition: Condition)
		extends ManyAuthTokensWithScopesAccess with SubView
}

/**
  * Common trait for access points which return multiple authentication tokens at a time and include scope information
  * @author Mikko Hilpinen
  * @since 27.10.2021, v2.0
  */
trait ManyAuthTokensWithScopesAccess
	extends ManyAuthTokensAccessLike[AuthTokenWithScopes, ManyAuthTokensWithScopesAccess]
{
	// COMPUTED ---------------------------------
	
	/**
	  * @return Model used for interacting with scope data in the DB
	  */
	protected def scopeModel = ScopeModel
	
	/**
	  * @param connection Implicit DB Connection
	  * @return Scope ids visible from this access point
	  */
	def scopeIds(implicit connection: Connection) = pullColumn(scopeModel.index).flatMap { _.int }.toSet
	
	
	// IMPLEMENTED  -----------------------------
	
	override def factory = AuthTokenWithScopesFactory
	
	override protected def _filter(condition: Condition): ManyAuthTokensWithScopesAccess =
		new SubAccess(this, condition)
		
	
	// OTHER    ----------------------------------
	
	/**
	  * @param serviceId Id of the targeted service
	  * @return An access point to tokens that are used with that service
	  */
	def forServiceWithId(serviceId: Int) =
		filter(scopeModel.withServiceId(serviceId).toCondition)
	
	/**
	  * @param serviceIds Ids of the targeted service
	  * @return An access point to those of these scopes which are linked to one of those services
	  */
	def forAnyOfServices(serviceIds: Iterable[Int]) =
		filter(scopeModel.serviceIdColumn in serviceIds)
}
