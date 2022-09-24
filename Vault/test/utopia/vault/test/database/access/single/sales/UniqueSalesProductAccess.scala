package utopia.vault.test.database.access.single.sales

import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.generic.model.immutable.Value
import utopia.vault.database.Connection
import utopia.vault.nosql.access.single.model.SingleRowModelAccess
import utopia.vault.nosql.access.template.model.DistinctModelAccess
import utopia.vault.nosql.template.Indexed
import utopia.vault.test.database.factory.sales.SalesProductFactory
import utopia.vault.test.database.model.sales.SalesProductModel
import utopia.vault.test.model.stored.sales.SalesProduct

import java.time.Instant

/**
  * A common trait for access points that return individual and distinct sales products.
  * @author Mikko Hilpinen
  * @since 28.02.2022, v1.12.1
  */
trait UniqueSalesProductAccess 
	extends SingleRowModelAccess[SalesProduct] 
		with DistinctModelAccess[SalesProduct, Option[SalesProduct], Value] with Indexed
{
	// COMPUTED	--------------------
	
	/**
	  * The name of this instance. None if no instance (or value) was found.
	  */
	def name(implicit connection: Connection) = pullColumn(model.nameColumn).string
	
	/**
	  * The unit price of this instance. None if no instance (or value) was found.
	  */
	def unitPrice(implicit connection: Connection) = pullColumn(model.unitPriceColumn).double
	
	/**
	  * Time when this sales product was first created. None if no instance (or value) was found.
	  */
	def created(implicit connection: Connection) = pullColumn(model.createdColumn).instant
	
	/**
	  * The vip only of this instance. None if no instance (or value) was found.
	  */
	def vipOnly(implicit connection: Connection) = pullColumn(model.vipOnlyColumn).boolean
	
	def id(implicit connection: Connection) = pullColumn(index).int
	
	/**
	  * Factory used for constructing database the interaction models
	  */
	protected def model = SalesProductModel
	
	
	// IMPLEMENTED	--------------------
	
	override def factory = SalesProductFactory
	
	
	// OTHER	--------------------
	
	/**
	  * Updates the creation times of the targeted sales products
	  * @param newCreated A new created to assign
	  * @return Whether any sales product was affected
	  */
	def created_=(newCreated: Instant)(implicit connection: Connection) = 
		putColumn(model.createdColumn, newCreated)
	
	/**
	  * Updates the names of the targeted sales products
	  * @param newName A new name to assign
	  * @return Whether any sales product was affected
	  */
	def name_=(newName: String)(implicit connection: Connection) = putColumn(model.nameColumn, newName)
	
	/**
	  * Updates the unit prices of the targeted sales products
	  * @param newUnitPrice A new unit price to assign
	  * @return Whether any sales product was affected
	  */
	def unitPrice_=(newUnitPrice: Double)(implicit connection: Connection) = 
		putColumn(model.unitPriceColumn, newUnitPrice)
	
	/**
	  * Updates the vips only of the targeted sales products
	  * @param newVipOnly A new vip only to assign
	  * @return Whether any sales product was affected
	  */
	def vipOnly_=(newVipOnly: Boolean)(implicit connection: Connection) = 
		putColumn(model.vipOnlyColumn, newVipOnly)
}

