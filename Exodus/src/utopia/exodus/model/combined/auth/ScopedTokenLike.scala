package utopia.exodus.model.combined.auth

import utopia.exodus.model.partial.auth.TokenData
import utopia.exodus.model.stored.auth.Token
import utopia.flow.util.Extender

/**
  * A common trait for combinations which include both token and scope id data
  * @author Mikko Hilpinen
  * @since 19.2.2022, v4.0
  */
trait ScopedTokenLike extends Extender[TokenData]
{
	// ABSTRACT --------------------------
	
	/**
	  * @return Wrapped token
	  */
	def token: Token
	
	/**
	  * @return Ids of the scopes accessible using this token
	  */
	def scopeIds: Set[Int]
	
	
	// COMPUTED -------------------------
	
	/**
	  * @return Id of this token
	  */
	def id = token.id
	
	
	// IMPLEMENTED  ---------------------
	
	override def wrapped = token.data
}
