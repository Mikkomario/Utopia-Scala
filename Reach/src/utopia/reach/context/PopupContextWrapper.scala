package utopia.reach.context
import utopia.firmament.context.TextContext
import utopia.paradigm.color.Color

/**
  * A common trait for context classes which wrap a popup context instance
  * @author Mikko Hilpinen
  * @since 17.4.2023, v1.0
  */
trait PopupContextWrapper[+Repr] extends PopupContextLike[Repr]
{
	// ABSTRACT ---------------------
	
	/**
	  * @return The wrapped popup context instance
	  */
	def popupContext: PopupContext
	
	/**
	  * @param base A new popup context to wrap
	  * @return A copy of this context with the specified popup context
	  */
	def withPopupContext(base: PopupContext): Repr
	
	
	// IMPLEMENTED  ----------------
	
	override def reachWindowContext: ReachWindowContext = popupContext
	override def textContext: TextContext = popupContext
	
	override def withReachWindowContext(base: ReachWindowContext): Repr =
		mapPopupContext { _.withReachWindowContext(base) }
	override def withTextContext(textContext: TextContext): Repr = mapPopupContext { _.withTextContext(textContext) }
	
	override def withWindowBackground(bg: Color) = mapPopupContext { _.withWindowBackground(bg) }
	override def withBackground(background: Color) = mapPopupContext { _.withBackground(background) }
	
	
	// OTHER    --------------------
	
	/**
	  * @param f A mapping function for the wrapped popup context
	  * @return A copy of this context with mapped popup context
	  */
	def mapPopupContext(f: PopupContext => PopupContext) = withPopupContext(f(popupContext))
}
