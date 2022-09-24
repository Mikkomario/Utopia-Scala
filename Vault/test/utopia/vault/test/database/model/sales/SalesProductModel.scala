package utopia.vault.test.database.model.sales

import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.generic.model.immutable.Value
import utopia.vault.model.immutable.StorableWithFactory
import utopia.vault.nosql.storable.DataInserter
import utopia.vault.test.database.factory.sales.SalesProductFactory
import utopia.vault.test.model.partial.sales.SalesProductData
import utopia.vault.test.model.stored.sales.SalesProduct

import java.time.Instant

/**
  * Used for constructing SalesProductModel instances and for inserting sales products to the database
  * @author Mikko Hilpinen
  * @since 28.02.2022, v1.12.1
  */
object SalesProductModel extends DataInserter[SalesProductModel, SalesProduct, SalesProductData]
{
	// ATTRIBUTES	--------------------
	
	/**
	  * Name of the property that contains sales product name
	  */
	val nameAttName = "name"
	
	/**
	  * Name of the property that contains sales product unit price
	  */
	val unitPriceAttName = "unitPrice"
	
	/**
	  * Name of the property that contains sales product created
	  */
	val createdAttName = "created"
	
	/**
	  * Name of the property that contains sales product vip only
	  */
	val vipOnlyAttName = "vipOnly"
	
	
	// COMPUTED	--------------------
	
	/**
	  * Column that contains sales product name
	  */
	def nameColumn = table(nameAttName)
	
	/**
	  * Column that contains sales product unit price
	  */
	def unitPriceColumn = table(unitPriceAttName)
	
	/**
	  * Column that contains sales product created
	  */
	def createdColumn = table(createdAttName)
	
	/**
	  * Column that contains sales product vip only
	  */
	def vipOnlyColumn = table(vipOnlyAttName)
	
	/**
	  * The factory object used by this model type
	  */
	def factory = SalesProductFactory
	
	
	// IMPLEMENTED	--------------------
	
	override def table = factory.table
	
	override def apply(data: SalesProductData) = 
		apply(None, Some(data.name), Some(data.unitPrice), Some(data.created), Some(data.vipOnly))
	
	override def complete(id: Value, data: SalesProductData) = SalesProduct(id.getInt, data)
	
	
	// OTHER	--------------------
	
	/**
	  * @param created Time when this sales product was first created
	  * @return A model containing only the specified created
	  */
	def withCreated(created: Instant) = apply(created = Some(created))
	
	/**
	  * @param id A sales product id
	  * @return A model with that id
	  */
	def withId(id: Int) = apply(Some(id))
	
	/**
	  * @return A model containing only the specified name
	  */
	def withName(name: String) = apply(name = Some(name))
	
	/**
	  * @return A model containing only the specified unit price
	  */
	def withUnitPrice(unitPrice: Double) = apply(unitPrice = Some(unitPrice))
	
	/**
	  * @return A model containing only the specified vip only
	  */
	def withVipOnly(vipOnly: Boolean) = apply(vipOnly = Some(vipOnly))
}

/**
  * Used for interacting with SalesProducts in the database
  * @param id sales product database id
  * @param created Time when this sales product was first created
  * @author Mikko Hilpinen
  * @since 28.02.2022, v1.12.1
  */
case class SalesProductModel(id: Option[Int] = None, name: Option[String] = None, 
	unitPrice: Option[Double] = None, created: Option[Instant] = None, vipOnly: Option[Boolean] = None) 
	extends StorableWithFactory[SalesProduct]
{
	// IMPLEMENTED	--------------------
	
	override def factory = SalesProductModel.factory
	
	override def valueProperties = {
		import SalesProductModel._
		Vector("id" -> id, nameAttName -> name, unitPriceAttName -> unitPrice, createdAttName -> created, 
			vipOnlyAttName -> vipOnly)
	}
	
	
	// OTHER	--------------------
	
	/**
	  * @param created A new created
	  * @return A new copy of this model with the specified created
	  */
	def withCreated(created: Instant) = copy(created = Some(created))
	
	/**
	  * @param name A new name
	  * @return A new copy of this model with the specified name
	  */
	def withName(name: String) = copy(name = Some(name))
	
	/**
	  * @param unitPrice A new unit price
	  * @return A new copy of this model with the specified unit price
	  */
	def withUnitPrice(unitPrice: Double) = copy(unitPrice = Some(unitPrice))
	
	/**
	  * @param vipOnly A new vip only
	  * @return A new copy of this model with the specified vip only
	  */
	def withVipOnly(vipOnly: Boolean) = copy(vipOnly = Some(vipOnly))
}

