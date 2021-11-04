package utopia.citadel.database.access.single.description

import utopia.citadel.database.factory.description.DescriptionLinkFactory
import utopia.citadel.database.model.description.DescriptionLinkModelFactory
import utopia.citadel.model.cached.DescriptionLinkTable
import utopia.metropolis.model.stored.description.DescriptionLink
import utopia.vault.database.Connection
import utopia.vault.nosql.access.single.model.SingleRowModelAccess
import utopia.vault.nosql.access.single.model.distinct.SingleIntIdModelAccess
import utopia.vault.nosql.template.Indexed
import utopia.vault.sql.Condition

object DescriptionLinkAccess
{
	// OTHER    ---------------------------------
	
	/**
	  * @param factory A factory used for reading description links
	  * @param model A factory used for constructing description link db models
	  * @param condition A condition to apply to the returned results
	  * @return An access point to the links available through that factory
	  */
	def apply(factory: DescriptionLinkFactory, model: DescriptionLinkModelFactory,
	          condition: Option[Condition] = None): DescriptionLinkAccess =
		new SimpleDescriptionLinkAccess(factory, model, condition)
	
	/**
	  * @param table A description link table
	  * @return An access point to description links in that table
	  */
	def apply(table: DescriptionLinkTable): DescriptionLinkAccess =
		apply(DescriptionLinkFactory(table), DescriptionLinkModelFactory(table))
	
	
	// NESTED   ---------------------------------
	
	private class SimpleDescriptionLinkAccess(override val factory: DescriptionLinkFactory,
	                                          override val model: DescriptionLinkModelFactory,
	                                          override val globalCondition: Option[Condition])
		extends DescriptionLinkAccess
}

/**
  * Common trait for access points which target description links
  * @author Mikko Hilpinen
  * @since 23.10.2021, v2.0
  */
trait DescriptionLinkAccess extends SingleRowModelAccess[DescriptionLink] with Indexed
{
	// ABSTRACT ------------------------------
	
	override def factory: DescriptionLinkFactory
	
	/**
	  * @return Model used for interacting with the link table
	  */
	protected def model: DescriptionLinkModelFactory
	
	
	// OTHER    ------------------------------
	
	/**
	  * @param id A description link id
	  * @return An access point to that description link in the database
	  */
	def apply(id: Int) = new SingleIdDescriptionLinkAccess(id)
	
	/**
	  * @param descriptionId Id of the targeted description
	  * @param connection Implicit DB Connection
	  * @return Description link referencing that description, if found
	  */
	def withDescriptionId(descriptionId: Int)(implicit connection: Connection) =
		find(model.withDescriptionId(descriptionId).toCondition)
	
	
	// NESTED   -------------------------------
	
	class SingleIdDescriptionLinkAccess(override val id: Int) extends SingleIntIdModelAccess[DescriptionLink]
	{
		// COMPUTED ---------------------------
		
		/**
		  * @param connection Implicit DB Connection
		  * @return Id of the linked described item
		  */
		def targetId(implicit connection: Connection) = pullColumn(model.targetIdColumn).getInt
		/**
		  * @param connection Implicit DB Connection
		  * @return Id of the linked description
		  */
		def descriptionId(implicit connection: Connection) = pullColumn(model.descriptionIdColumn).getInt
		
		
		// IMPLEMENTED  -----------------------
		
		override def factory = DescriptionLinkAccess.this.factory
	}
}
