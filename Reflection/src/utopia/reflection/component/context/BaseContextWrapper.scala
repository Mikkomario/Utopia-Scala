package utopia.reflection.component.context

/**
  * A common traits for wrappers around the base context
  * @author Mikko Hilpinen
  * @since 27.4.2020, v1.2
  */
trait BaseContextWrapper extends BaseContextLike
{
	// ABSTRACT	------------------------
	
	/**
	  * @return The wrapped base context
	  */
	def base: BaseContextLike
	
	
	// IMPLEMENTED	--------------------
	
	override def actorHandler = base.actorHandler
	
	override def defaultFont = base.defaultFont
	
	override def defaultColorScheme = base.defaultColorScheme
	
	override def margins = base.margins
	
	override def allowImageUpscaling = base.allowImageUpscaling
	
	override def defaultStackMargin = base.defaultStackMargin
	
	override def relatedItemsStackMargin = base.relatedItemsStackMargin
}
