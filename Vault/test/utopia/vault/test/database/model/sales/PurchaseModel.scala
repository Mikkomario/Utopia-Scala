package utopia.vault.test.database.model.sales

import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.generic.model.immutable.Value
import utopia.vault.model.immutable.StorableWithFactory
import utopia.vault.nosql.storable.DataInserter
import utopia.vault.test.database.factory.sales.PurchaseFactory
import utopia.vault.test.model.partial.sales.PurchaseData
import utopia.vault.test.model.stored.sales.Purchase

import java.time.{Instant, LocalDate}

/**
  * Used for constructing PurchaseModel instances and for inserting purchases to the database
  * @author Mikko Hilpinen
  * @since 28.02.2022, v1.12.1
  */
object PurchaseModel extends DataInserter[PurchaseModel, Purchase, PurchaseData]
{
	// ATTRIBUTES	--------------------
	
	/**
	  * Name of the property that contains purchase product id
	  */
	val productIdAttName = "productId"
	
	/**
	  * Name of the property that contains purchase units bought
	  */
	val unitsBoughtAttName = "unitsBought"
	
	/**
	  * Name of the property that contains purchase estimated delivery
	  */
	val estimatedDeliveryAttName = "estimatedDelivery"
	
	/**
	  * Name of the property that contains purchase created
	  */
	val createdAttName = "created"
	
	
	// COMPUTED	--------------------
	
	/**
	  * Column that contains purchase product id
	  */
	def productIdColumn = table(productIdAttName)
	
	/**
	  * Column that contains purchase units bought
	  */
	def unitsBoughtColumn = table(unitsBoughtAttName)
	
	/**
	  * Column that contains purchase estimated delivery
	  */
	def estimatedDeliveryColumn = table(estimatedDeliveryAttName)
	
	/**
	  * Column that contains purchase created
	  */
	def createdColumn = table(createdAttName)
	
	/**
	  * The factory object used by this model type
	  */
	def factory = PurchaseFactory
	
	
	// IMPLEMENTED	--------------------
	
	override def table = factory.table
	
	override def apply(data: PurchaseData) = 
		apply(None, Some(data.productId), Some(data.unitsBought), data.estimatedDelivery, Some(data.created))
	
	override def complete(id: Value, data: PurchaseData) = Purchase(id.getInt, data)
	
	
	// OTHER	--------------------
	
	/**
	  * @param created Time when this purchase was first created
	  * @return A model containing only the specified created
	  */
	def withCreated(created: Instant) = apply(created = Some(created))
	
	/**
	  * @return A model containing only the specified estimated delivery
	  */
	def withEstimatedDelivery(estimatedDelivery: LocalDate) = apply(estimatedDelivery = Some(estimatedDelivery))
	
	/**
	  * @param id A purchase id
	  * @return A model with that id
	  */
	def withId(id: Int) = apply(Some(id))
	
	/**
	  * @param productId Id of the sales product linked with this purchase
	  * @return A model containing only the specified product id
	  */
	def withProductId(productId: Int) = apply(productId = Some(productId))
	
	/**
	  * @return A model containing only the specified units bought
	  */
	def withUnitsBought(unitsBought: Int) = apply(unitsBought = Some(unitsBought))
}

/**
  * Used for interacting with Purchases in the database
  * @param id purchase database id
  * @param productId Id of the sales product linked with this purchase
  * @param created Time when this purchase was first created
  * @author Mikko Hilpinen
  * @since 28.02.2022, v1.12.1
  */
case class PurchaseModel(id: Option[Int] = None, productId: Option[Int] = None, 
	unitsBought: Option[Int] = None, estimatedDelivery: Option[LocalDate] = None, 
	created: Option[Instant] = None) 
	extends StorableWithFactory[Purchase]
{
	// IMPLEMENTED	--------------------
	
	override def factory = PurchaseModel.factory
	
	override def valueProperties = {
		import PurchaseModel._
		Vector("id" -> id, productIdAttName -> productId, unitsBoughtAttName -> unitsBought, 
			estimatedDeliveryAttName -> estimatedDelivery, createdAttName -> created)
	}
	
	
	// OTHER	--------------------
	
	/**
	  * @param created A new created
	  * @return A new copy of this model with the specified created
	  */
	def withCreated(created: Instant) = copy(created = Some(created))
	
	/**
	  * @param estimatedDelivery A new estimated delivery
	  * @return A new copy of this model with the specified estimated delivery
	  */
	def withEstimatedDelivery(estimatedDelivery: LocalDate) = copy(estimatedDelivery = Some(estimatedDelivery))
	
	/**
	  * @param productId A new product id
	  * @return A new copy of this model with the specified product id
	  */
	def withProductId(productId: Int) = copy(productId = Some(productId))
	
	/**
	  * @param unitsBought A new units bought
	  * @return A new copy of this model with the specified units bought
	  */
	def withUnitsBought(unitsBought: Int) = copy(unitsBought = Some(unitsBought))
}

