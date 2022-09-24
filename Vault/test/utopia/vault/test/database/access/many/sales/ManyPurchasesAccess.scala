package utopia.vault.test.database.access.many.sales

import utopia.flow.generic.casting.ValueConversions._
import utopia.vault.database.Connection
import utopia.vault.nosql.access.many.model.ManyRowModelAccess
import utopia.vault.nosql.template.Indexed
import utopia.vault.nosql.view.{ChronoRowFactoryView, SubView}
import utopia.vault.sql.Condition
import utopia.vault.test.database.factory.sales.PurchaseFactory
import utopia.vault.test.database.model.sales.PurchaseModel
import utopia.vault.test.model.stored.sales.Purchase

import java.time.{Instant, LocalDate}

object ManyPurchasesAccess
{
	// NESTED	--------------------
	
	private class ManyPurchasesSubView(override val parent: ManyRowModelAccess[Purchase], 
		override val filterCondition: Condition) 
		extends ManyPurchasesAccess with SubView
}

/**
  * A common trait for access points which target multiple purchases at a time
  * @author Mikko Hilpinen
  * @since 28.02.2022, v1.12.1
  */
trait ManyPurchasesAccess 
	extends ManyRowModelAccess[Purchase] with ChronoRowFactoryView[Purchase, ManyPurchasesAccess] with Indexed
{
	// COMPUTED	--------------------
	
	/**
	  * product ids of the accessible purchases
	  */
	def productIds(implicit connection: Connection) = pullColumn(model.productIdColumn).map { v => v.getInt }
	
	/**
	  * units boughts of the accessible purchases
	  */
	def unitsBoughts(implicit connection: Connection) = pullColumn(model.unitsBoughtColumn)
		.map { v => v.getInt }
	
	/**
	  * estimated deliverys of the accessible purchases
	  */
	def estimatedDeliverys(implicit connection: Connection) = 
		pullColumn(model.estimatedDeliveryColumn).flatMap { _.localDate }
	
	/**
	  * creation times of the accessible purchases
	  */
	def creationTimes(implicit connection: Connection) = pullColumn(model.createdColumn).map { _.getInstant }
	
	def ids(implicit connection: Connection) = pullColumn(index).flatMap { id => id.int }
	
	/**
	  * Factory used for constructing database the interaction models
	  */
	protected def model = PurchaseModel
	
	
	// IMPLEMENTED	--------------------
	
	override def factory = PurchaseFactory
	
	override def filter(additionalCondition: Condition): ManyPurchasesAccess = 
		new ManyPurchasesAccess.ManyPurchasesSubView(this, additionalCondition)
	
	
	// OTHER	--------------------
	
	/**
	  * Updates the creation times of the targeted purchases
	  * @param newCreated A new created to assign
	  * @return Whether any purchase was affected
	  */
	def creationTimes_=(newCreated: Instant)(implicit connection: Connection) = 
		putColumn(model.createdColumn, newCreated)
	
	/**
	  * Updates the estimated deliverys of the targeted purchases
	  * @param newEstimatedDelivery A new estimated delivery to assign
	  * @return Whether any purchase was affected
	  */
	def estimatedDeliverys_=(newEstimatedDelivery: LocalDate)(implicit connection: Connection) = 
		putColumn(model.estimatedDeliveryColumn, newEstimatedDelivery)
	
	/**
	  * Updates the product ids of the targeted purchases
	  * @param newProductId A new product id to assign
	  * @return Whether any purchase was affected
	  */
	def productIds_=(newProductId: Int)(implicit connection: Connection) = 
		putColumn(model.productIdColumn, newProductId)
	
	/**
	  * Updates the units boughts of the targeted purchases
	  * @param newUnitsBought A new units bought to assign
	  * @return Whether any purchase was affected
	  */
	def unitsBoughts_=(newUnitsBought: Int)(implicit connection: Connection) = 
		putColumn(model.unitsBoughtColumn, newUnitsBought)
}

