package utopia.reach.context
import utopia.firmament.context.text.StaticTextContext
import utopia.paradigm.color.Color

/**
  * A common trait for context classes which wrap a popup context instance
  * @author Mikko Hilpinen
  * @since 17.4.2023, v1.0
  */
trait ReachContentWindowContextWrapper[+Repr] extends ReachContentWindowContextLike[Repr]
{
	// ABSTRACT ---------------------
	
	/**
	  * @return The wrapped popup context instance
	  */
	def contentWindowContext: ReachContentWindowContext
	/**
	  * @param base A new popup context to wrap
	  * @return A copy of this context with the specified popup context
	  */
	def withContentWindowContext(base: ReachContentWindowContext): Repr
	
	
	// IMPLEMENTED  ----------------
	
	override def base: StaticTextContext = contentWindowContext.base
	override def reachWindowContext: ReachWindowContext = contentWindowContext
	
	override def withBase(base: StaticTextContext): Repr = mapContentWindowContext { _.withBase(base) }
	override def withReachWindowContext(base: ReachWindowContext): Repr =
		mapContentWindowContext { _.withReachWindowContext(base) }
	
	override def withWindowBackground(bg: Color) = mapContentWindowContext { _.withWindowBackground(bg) }
	override def withBackground(background: Color) = mapContentWindowContext { _.withBackground(background) }
	
	
	// OTHER    --------------------
	
	/**
	  * @param f A mapping function for the wrapped popup context
	  * @return A copy of this context with mapped popup context
	  */
	def mapContentWindowContext(f: ReachContentWindowContext => ReachContentWindowContext) =
		withContentWindowContext(f(contentWindowContext))
}
