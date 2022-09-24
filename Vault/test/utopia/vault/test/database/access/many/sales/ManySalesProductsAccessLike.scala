package utopia.vault.test.database.access.many.sales

import utopia.flow.generic.casting.ValueConversions._
import utopia.vault.database.Connection
import utopia.vault.nosql.access.many.model.ManyModelAccess
import utopia.vault.nosql.template.Indexed
import utopia.vault.nosql.view.FilterableView
import utopia.vault.test.database.model.sales.SalesProductModel

import java.time.Instant

/**
  * A common trait for access points which target multiple sales products or similar instances at a time
  * @author Mikko Hilpinen
  * @since 28.02.2022, v1.12.1
  */
trait ManySalesProductsAccessLike[+A, +Repr <: ManyModelAccess[A]] 
	extends ManyModelAccess[A] with Indexed with FilterableView[Repr]
{
	// COMPUTED	--------------------
	
	/**
	  * names of the accessible sales products
	  */
	def names(implicit connection: Connection) = pullColumn(model.nameColumn).map { v => v.getString }
	
	/**
	  * unit prices of the accessible sales products
	  */
	def unitPrices(implicit connection: Connection) = pullColumn(model.unitPriceColumn)
		.map { v => v.getDouble }
	
	/**
	  * creation times of the accessible sales products
	  */
	def creationTimes(implicit connection: Connection) = pullColumn(model.createdColumn).map { _.getInstant }
	
	/**
	  * vips only of the accessible sales products
	  */
	def vipsOnly(implicit connection: Connection) = pullColumn(model.vipOnlyColumn).map { v => v.getBoolean }
	
	def ids(implicit connection: Connection) = pullColumn(index).flatMap { id => id.int }
	
	/**
	  * Factory used for constructing database the interaction models
	  */
	protected def model = SalesProductModel
	
	
	// OTHER	--------------------
	
	/**
	  * Updates the creation times of the targeted sales products
	  * @param newCreated A new created to assign
	  * @return Whether any sales product was affected
	  */
	def creationTimes_=(newCreated: Instant)(implicit connection: Connection) = 
		putColumn(model.createdColumn, newCreated)
	
	/**
	  * Updates the names of the targeted sales products
	  * @param newName A new name to assign
	  * @return Whether any sales product was affected
	  */
	def names_=(newName: String)(implicit connection: Connection) = putColumn(model.nameColumn, newName)
	
	/**
	  * Updates the unit prices of the targeted sales products
	  * @param newUnitPrice A new unit price to assign
	  * @return Whether any sales product was affected
	  */
	def unitPrices_=(newUnitPrice: Double)(implicit connection: Connection) = 
		putColumn(model.unitPriceColumn, newUnitPrice)
	
	/**
	  * Updates the vips only of the targeted sales products
	  * @param newVipOnly A new vip only to assign
	  * @return Whether any sales product was affected
	  */
	def vipsOnly_=(newVipOnly: Boolean)(implicit connection: Connection) = 
		putColumn(model.vipOnlyColumn, newVipOnly)
}

