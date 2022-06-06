package utopia.vault.test.database.access.many.sales

import utopia.vault.nosql.access.many.model.ManyRowModelAccess
import utopia.vault.nosql.view.{ChronoRowFactoryView, SubView}
import utopia.vault.sql.Condition
import utopia.vault.test.database.factory.sales.SalesProductFactory
import utopia.vault.test.model.stored.sales.SalesProduct

object ManySalesProductsAccess
{
	// NESTED	--------------------
	
	private class ManySalesProductsSubView(override val parent: ManyRowModelAccess[SalesProduct], 
		override val filterCondition: Condition) 
		extends ManySalesProductsAccess with SubView
}

/**
  * A common trait for access points which target multiple sales products at a time
  * @author Mikko Hilpinen
  * @since 28.02.2022, v1.12.1
  */
trait ManySalesProductsAccess 
	extends ManySalesProductsAccessLike[SalesProduct, ManySalesProductsAccess] 
		with ManyRowModelAccess[SalesProduct] with ChronoRowFactoryView[SalesProduct, ManySalesProductsAccess]
{
	// IMPLEMENTED	--------------------
	
	override def factory = SalesProductFactory
	
	override def filter(additionalCondition: Condition): ManySalesProductsAccess = 
		new ManySalesProductsAccess.ManySalesProductsSubView(this, additionalCondition)
}

