package utopia.ambassador.database.model.token

import utopia.ambassador.database.factory.token.AuthTokenFactory
import utopia.ambassador.model.partial.token.AuthTokenData
import utopia.ambassador.model.stored.token.AuthToken
import utopia.flow.datastructure.immutable.Value
import utopia.flow.generic.ValueConversions._
import utopia.flow.time.Now
import utopia.vault.model.immutable.StorableWithFactory
import utopia.vault.model.template.DataInserter
import utopia.vault.nosql.template.Deprecatable
import utopia.vault.sql.SqlExtensions._

import java.time.Instant

object AuthTokenModel extends DataInserter[AuthTokenModel, AuthToken, AuthTokenData] with Deprecatable
{
	// ATTRIBUTES   -------------------------------
	
	/**
	  * Name of the property that contains deprecation timestamp
	  */
	val deprecationAttName = "deprecatedAfter"
	/**
	  * Name of the property that contains token expiration time
	  */
	val expirationAttName = "expiration"
	
	
	// COMPUTED -----------------------------------
	
	/**
	  * @return The factory used by this class / model
	  */
	def factory = AuthTokenFactory
	
	/**
	  * Column that contains deprecation timestamp
	  */
	def deprecationColumn = table(deprecationAttName)
	/**
	  * Column that contains token expiration time
	  */
	def expirationColumn = table(expirationAttName)
	
	
	// IMPLEMENTED  -------------------------------
	
	override def table = factory.table
	
	override def nonDeprecatedCondition = deprecationColumn.isNull && expirationColumn > Now.toValue
	
	override def apply(data: AuthTokenData) =
		apply(None, Some(data.userId), Some(data.token), Some(data.isRefreshToken), Some(data.created),
			data.expiration, data.deprecatedAfter)
	
	override protected def complete(id: Value, data: AuthTokenData) = AuthToken(id.getInt, data)
	
	
	// OTHER    -----------------------------------
	
	/**
	  * @param userId Id of the token owner
	  * @return A model with that user id
	  */
	def withUserId(userId: Int) = apply(userId = Some(userId))
}

/**
  * Used for interacting with token data in the DB
  * @author Mikko Hilpinen
  * @since 11.7.2021, v1.0
  */
case class AuthTokenModel(id: Option[Int] = None, userId: Option[Int] = None, token: Option[String] = None,
                          isRefreshToken: Option[Boolean] = None, created: Option[Instant] = None,
                          expiration: Option[Instant] = None, deprecatedAfter: Option[Instant] = None)
	extends StorableWithFactory[AuthToken]
{
	import AuthTokenModel._
	
	override def factory = AuthTokenModel.factory
	
	override def valueProperties = Vector("id" -> id, "userId" -> userId, "token" -> token,
		"isRefreshToken" -> isRefreshToken, "created" -> created, expirationAttName -> expiration,
		deprecationAttName -> deprecatedAfter)
}