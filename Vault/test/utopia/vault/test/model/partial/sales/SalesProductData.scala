package utopia.vault.test.model.partial.sales

import utopia.flow.collection.value.typeless.Model
import utopia.flow.generic.ModelConvertible
import utopia.flow.generic.ValueConversions._
import utopia.flow.time.Now

import java.time.Instant

/**
  * @param created Time when this sales product was first created
  */
case class SalesProductData(name: String, unitPrice: Double, created: Instant = Now, 
	vipOnly: Boolean = false) 
	extends ModelConvertible
{
	// IMPLEMENTED	--------------------
	
	override def toModel = 
		Model(Vector("name" -> name, "unit_price" -> unitPrice, "created" -> created, "vip_only" -> vipOnly))
}

