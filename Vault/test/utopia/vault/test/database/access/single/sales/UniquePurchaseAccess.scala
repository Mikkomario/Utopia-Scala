package utopia.vault.test.database.access.single.sales

import utopia.flow.collection.value.typeless.Value
import utopia.flow.generic.ValueConversions._
import utopia.vault.database.Connection
import utopia.vault.nosql.access.single.model.SingleRowModelAccess
import utopia.vault.nosql.access.template.model.DistinctModelAccess
import utopia.vault.nosql.template.Indexed
import utopia.vault.test.database.factory.sales.PurchaseFactory
import utopia.vault.test.database.model.sales.PurchaseModel
import utopia.vault.test.model.stored.sales.Purchase

import java.time.{Instant, LocalDate}

/**
  * A common trait for access points that return individual and distinct purchases.
  * @author Mikko Hilpinen
  * @since 28.02.2022, v1.12.1
  */
trait UniquePurchaseAccess 
	extends SingleRowModelAccess[Purchase] with DistinctModelAccess[Purchase, Option[Purchase], Value] 
		with Indexed
{
	// COMPUTED	--------------------
	
	/**
	  * Id of the sales product linked with this purchase. None if no instance (or value) was found.
	  */
	def productId(implicit connection: Connection) = pullColumn(model.productIdColumn).int
	
	/**
	  * The units bought of this instance. None if no instance (or value) was found.
	  */
	def unitsBought(implicit connection: Connection) = pullColumn(model.unitsBoughtColumn).int
	
	/**
	  * The estimated delivery of this instance. None if no instance (or value) was found.
	  */
	def estimatedDelivery(implicit connection: Connection) = pullColumn(model
		.estimatedDeliveryColumn).localDate
	
	/**
	  * Time when this purchase was first created. None if no instance (or value) was found.
	  */
	def created(implicit connection: Connection) = pullColumn(model.createdColumn).instant
	
	def id(implicit connection: Connection) = pullColumn(index).int
	
	/**
	  * Factory used for constructing database the interaction models
	  */
	protected def model = PurchaseModel
	
	
	// IMPLEMENTED	--------------------
	
	override def factory = PurchaseFactory
	
	
	// OTHER	--------------------
	
	/**
	  * Updates the creation times of the targeted purchases
	  * @param newCreated A new created to assign
	  * @return Whether any purchase was affected
	  */
	def created_=(newCreated: Instant)(implicit connection: Connection) = 
		putColumn(model.createdColumn, newCreated)
	
	/**
	  * Updates the estimated deliverys of the targeted purchases
	  * @param newEstimatedDelivery A new estimated delivery to assign
	  * @return Whether any purchase was affected
	  */
	def estimatedDelivery_=(newEstimatedDelivery: LocalDate)(implicit connection: Connection) = 
		putColumn(model.estimatedDeliveryColumn, newEstimatedDelivery)
	
	/**
	  * Updates the product ids of the targeted purchases
	  * @param newProductId A new product id to assign
	  * @return Whether any purchase was affected
	  */
	def productId_=(newProductId: Int)(implicit connection: Connection) = 
		putColumn(model.productIdColumn, newProductId)
	
	/**
	  * Updates the units boughts of the targeted purchases
	  * @param newUnitsBought A new units bought to assign
	  * @return Whether any purchase was affected
	  */
	def unitsBought_=(newUnitsBought: Int)(implicit connection: Connection) = 
		putColumn(model.unitsBoughtColumn, newUnitsBought)
}

