package utopia.exodus.database.access.single.auth

import java.time.Instant
import utopia.exodus.database.factory.auth.TokenScopeLinkFactory
import utopia.exodus.database.model.auth.TokenScopeLinkModel
import utopia.exodus.model.stored.auth.TokenScopeLink
import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.generic.model.immutable.Value
import utopia.vault.database.Connection
import utopia.vault.nosql.access.single.model.SingleRowModelAccess
import utopia.vault.nosql.access.template.model.DistinctModelAccess
import utopia.vault.nosql.template.Indexed

/**
  * A common trait for access points that return individual and distinct token scope links.
  * @author Mikko Hilpinen
  * @since 18.02.2022, v4.0
  */
trait UniqueTokenScopeLinkAccess 
	extends SingleRowModelAccess[TokenScopeLink] 
		with DistinctModelAccess[TokenScopeLink, Option[TokenScopeLink], Value] with Indexed
{
	// COMPUTED	--------------------
	
	/**
	  * Id of the linked token. None if no instance (or value) was found.
	  */
	def tokenId(implicit connection: Connection) = pullColumn(model.tokenIdColumn).int
	
	/**
	  * Id of the enabled scope. None if no instance (or value) was found.
	  */
	def scopeId(implicit connection: Connection) = pullColumn(model.scopeIdColumn).int
	
	/**
	  * Time when this token scope link was first created. None if no instance (or value) was found.
	  */
	def created(implicit connection: Connection) = pullColumn(model.createdColumn).instant
	
	/**
	  * Whether the linked scope is directly accessible using the linked token. None if no instance (or value)
	  *  was found.
	  */
	def isDirectlyAccessible(implicit connection: Connection) = 
		pullColumn(model.isDirectlyAccessibleColumn).boolean
	
	/**
	  * 
		Whether this scope is granted to tokens that are created using this token. None if no instance (or value)
	  *  was found.
	  */
	def grantsForward(implicit connection: Connection) = pullColumn(model.grantsForwardColumn).boolean
	
	def id(implicit connection: Connection) = pullColumn(index).int
	
	/**
	  * Factory used for constructing database the interaction models
	  */
	protected def model = TokenScopeLinkModel
	
	
	// IMPLEMENTED	--------------------
	
	override def factory = TokenScopeLinkFactory
	
	
	// OTHER	--------------------
	
	/**
	  * Updates the creation times of the targeted token scope links
	  * @param newCreated A new created to assign
	  * @return Whether any token scope link was affected
	  */
	def created_=(newCreated: Instant)(implicit connection: Connection) = 
		putColumn(model.createdColumn, newCreated)
	
	/**
	  * Updates the grant forward of the targeted token scope links
	  * @param newGrantsForward A new grants forward to assign
	  * @return Whether any token scope link was affected
	  */
	def grantsForward_=(newGrantsForward: Boolean)(implicit connection: Connection) = 
		putColumn(model.grantsForwardColumn, newGrantsForward)
	
	/**
	  * Updates the are directly accessible of the targeted token scope links
	  * @param newIsDirectlyAccessible A new is directly accessible to assign
	  * @return Whether any token scope link was affected
	  */
	def isDirectlyAccessible_=(newIsDirectlyAccessible: Boolean)(implicit connection: Connection) = 
		putColumn(model.isDirectlyAccessibleColumn, newIsDirectlyAccessible)
	
	/**
	  * Updates the scopes ids of the targeted token scope links
	  * @param newScopeId A new scope id to assign
	  * @return Whether any token scope link was affected
	  */
	def scopeId_=(newScopeId: Int)(implicit connection: Connection) = putColumn(model.scopeIdColumn, 
		newScopeId)
	
	/**
	  * Updates the tokens ids of the targeted token scope links
	  * @param newTokenId A new token id to assign
	  * @return Whether any token scope link was affected
	  */
	def tokenId_=(newTokenId: Int)(implicit connection: Connection) = putColumn(model.tokenIdColumn, 
		newTokenId)
}

