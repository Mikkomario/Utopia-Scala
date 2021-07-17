package utopia.ambassador.database.model.scope

import utopia.ambassador.database.AmbassadorTables
import utopia.flow.generic.ValueConversions._
import utopia.vault.database.Connection
import utopia.vault.model.immutable.Storable
import utopia.vault.sql.Insert

object AuthPreparationScopeLinkModel
{
	// COMPUTED -----------------------------
	
	/**
	  * @return The table used by this model type
	  */
	def table = AmbassadorTables.scopeRequestPreparation
	
	
	// OTHER    -----------------------------
	
	/**
	  * Inserts a new link to the DB
	  * @param preparationId Id of the described authentication preparation
	  * @param scopeId Id of the scope linked to the preparation
	  * @param connection Implicit DB connection
	  * @return Id of the generated link
	  */
	def insert(preparationId: Int, scopeId: Int)(implicit connection: Connection) =
		apply(None, Some(preparationId), Some(scopeId)).insert().getInt
	/**
	  * Inserts new links to the DB
	  * @param links Links to insert where each item contains 1) described preparation's id and 2) linked scope's id
	  * @param connection Implicit DB connection
	  * @return Ids of the generated links
	  */
	def insert(links: Seq[(Int, Int)])(implicit connection: Connection) =
		Insert(table, links.map { case (preparationId, scopeId) =>
			apply(None, Some(preparationId), Some(scopeId)).toModel }).generatedIntKeys
	/**
	  * Inserts new links to the DB
	  * @param preparationId Id of the described authentication preparation
	  * @param scopeIds Ids of the scopes linked to the preparation
	  * @param connection Implicit DB connection
	  * @return Ids of the generated links
	  */
	def insert(preparationId: Int, scopeIds: Seq[Int])(implicit connection: Connection): Vector[Int] =
		insert(scopeIds.map { preparationId -> _ })
}

/**
  * Used for interacting with authentication preparation - scope links in the DB
  * @author Mikko Hilpinen
  * @since 18.7.2021, v1.0
  */
case class AuthPreparationScopeLinkModel(id: Option[Int] = None, preparationId: Option[Int] = None,
                                         scopeId: Option[Int] = None) extends Storable
{
	override def table = AuthPreparationScopeLinkModel.table
	
	override def valueProperties = Vector("id" -> id, "preparationId" -> preparationId, "scopeId" -> scopeId)
}
