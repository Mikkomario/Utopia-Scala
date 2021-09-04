package utopia.ambassador.database.model.token

import utopia.ambassador.database.AmbassadorTables
import utopia.flow.generic.ValueConversions._
import utopia.vault.database.Connection
import utopia.vault.model.immutable.Storable
import utopia.vault.sql.Insert

object TokenScopeLinkModel
{
	// ATTRIBUTES   ----------------------
	
	/**
	  * Name of the property that refers to the linked scope
	  */
	val scopeIdAttName = "scopeId"
	/**
	  * Name of the property that refers to the linked token
	  */
	val tokenIdAttName = "tokenId"
	
	
	// COMPUTED --------------------------
	
	/**
	  * @return The table used by this model / class
	  */
	def table = AmbassadorTables.authTokenScope
	
	/**
	  * Column that refers to linked scope
	  */
	def scopeIdColumn = table(scopeIdAttName)
	
	
	// OTHER    --------------------------
	
	/**
	  * @param tokenId Id of the linked token
	  * @return A model with that token id
	  */
	def withTokenId(tokenId: Int) = apply(tokenId = Some(tokenId))
	
	/**
	  * Inserts a new token-scope-link to the DB
	  * @param tokenId Id of the linked token
	  * @param scopeId Id of the linked scope
	  * @param connection DB Connection (implicit)
	  * @return Id of the generated link
	  */
	def insert(tokenId: Int, scopeId: Int)(implicit connection: Connection) =
		apply(None, Some(tokenId), Some(scopeId)).insert().getInt
	/**
	  * Inserts multiple new token-scope-links to the database
	  * @param data Data to insert, where each item is a tokenId-scopeId pair
	  * @param connection Implicit DB Connection
	  * @return Ids of the generated links
	  */
	def insert(data: Seq[(Int, Int)])(implicit connection: Connection) =
		Insert(table, data.map { case (tokenId, scopeId) => apply(None, Some(tokenId), Some(scopeId)).toModel })
			.generatedIntKeys
	/**
	  * Inserts multiple new token-scope-links to the database
	  * @param tokenId Id of the linked token
	  * @param scopeIds Ids of the linked scopes
	  * @param connection Implicit DB Connection
	  * @return Ids of the generated links
	  */
	def insert(tokenId: Int, scopeIds: Seq[Int])(implicit connection: Connection): Vector[Int] =
		insert(scopeIds.map { tokenId -> _ })
}

/**
  * Used for interacting with links between tokens and their scope
  * @author Mikko Hilpinen
  * @since 11.7.2021, v1.0
  */
case class TokenScopeLinkModel(id: Option[Int] = None, tokenId: Option[Int] = None, scopeId: Option[Int] = None)
	extends Storable
{
	import TokenScopeLinkModel._
	
	override def table = TokenScopeLinkModel.table
	
	override def valueProperties = Vector("id" -> id, tokenIdAttName -> tokenId, scopeIdAttName -> scopeId)
}