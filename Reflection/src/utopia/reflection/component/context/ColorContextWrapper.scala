package utopia.reflection.component.context

/**
  * A common trait for contexts that wrap the color context
  * @author Mikko Hilpinen
  * @since 27.4.2020, v1.2
  */
trait ColorContextWrapper extends ColorContextLike with BaseContextWrapper
{
	// ABSTRACT	------------------------
	
	override def base: ColorContextLike
	
	
	// IMPLEMENTED	--------------------
	
	override def colorScheme = base.colorScheme
	
	override def containerBackground = base.containerBackground
}
