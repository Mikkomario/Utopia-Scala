package utopia.exodus.model.combined.auth

import utopia.exodus.model.partial.auth.TokenData
import utopia.exodus.model.stored.auth.{Token, TokenScopeLink}
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
	  * @return Links between this token and the associated scopes
	  */
	def scopeLinks: Vector[TokenScopeLink]
	
	
	// COMPUTED -------------------------
	
	/**
	  * @return Id of this token
	  */
	def id = token.id
	
	/**
	  * @return Ids of the scopes accessible using this token
	  */
	def accessibleScopeIds = scopeLinks.filter { _.isDirectlyAccessible }.map { _.scopeId }.toSet
	/**
	  * @return Ids of the scopes that are granted to tokens generated using this token
	  */
	def forwardedScopeIds = scopeLinks.filter { _.grantsForward }.map { _.scopeId }.toSet
	/**
	  * @return Ids of the scopes that are accessible and/or granted forward
	  */
	def allScopeIds = scopeLinks.map { _.scopeId }.toSet
	
	
	// IMPLEMENTED  ---------------------
	
	override def wrapped = token.data
}
