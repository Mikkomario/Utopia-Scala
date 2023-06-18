package utopia.reach.component.factory
import utopia.flow.view.immutable.eventful.Fixed
import utopia.flow.view.template.eventful.Changing
import utopia.reach.component.hierarchy.ComponentHierarchy

object FromVariableContextComponentFactoryFactory
{
	/**
	  * Type alias for FromVariableContextComponentFactoryFactory
	  */
	type Vccff[-N, +CF] = FromVariableContextComponentFactoryFactory[N, CF]
}

/**
  * Common trait for component factory -factories that support variable context pointers
  * @author Mikko Hilpinen
  * @since 18.6.2023, v1.1
  * @tparam N Type of context accepted by this factory
  * @tparam CF Type of component factories produced by this factory
  */
trait FromVariableContextComponentFactoryFactory[-N, +CF] extends FromContextComponentFactoryFactory[N, CF]
{
	// ABSTRACT --------------------------
	
	/**
	  * @param hierarchy A component creation hierarchy
	  * @param context Variable component creation context
	  * @return A new component factory that uses the specified context pointer
	  */
	def withContextPointer(hierarchy: ComponentHierarchy, context: Changing[N]): CF
	
	
	// IMPLEMENTED  ----------------------
	
	override def withContext(hierarchy: ComponentHierarchy, context: N): CF =
		withContextPointer(hierarchy, Fixed(context))
}
