package utopia.vault.test.model.partial.sales

import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.generic.model.immutable.Model
import utopia.flow.generic.model.template.ModelConvertible
import utopia.flow.time.Now

import java.time.{Instant, LocalDate}

/**
  * @param productId Id of the sales product linked with this purchase
  * @param created Time when this purchase was first created
  */
case class PurchaseData(productId: Int, unitsBought: Int = 1, estimatedDelivery: Option[LocalDate] = None, 
	created: Instant = Now) 
	extends ModelConvertible
{
	// IMPLEMENTED	--------------------
	
	override def toModel = 
		Model(Vector("product_id" -> productId, "units_bought" -> unitsBought, 
			"estimated_delivery" -> estimatedDelivery, "created" -> created))
}

