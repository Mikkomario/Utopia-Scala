package utopia.reach.context

import utopia.firmament.context.TextContext
import utopia.paradigm.color.Color

object ReachContentWindowContext
{
	// IMPLICIT -------------------------
	
	/**
	  * @param textContext A text context to wrap
	  * @return A Pop-up creation context that uses the specified text context and default settings
	  */
	implicit def apply(textContext: TextContext): ReachContentWindowContext =
		apply(ReachWindowContext(textContext.actorHandler, textContext.background).borderless.windowed, textContext)
	
	
	// OTHER    -------------------------
	
	/**
	  * @param window Window creation context to wrap
	  * @param text Text context to wrap
	  * @return A new pop-up context
	  */
	def apply(window: ReachWindowContext, text: TextContext): ReachContentWindowContext = _ReachContentWindowContext(window, text)
	
	
	// NESTED   -------------------------
	
	private case class _ReachContentWindowContext(reachWindowContext: ReachWindowContext, textContext: TextContext)
		extends ReachContentWindowContext
	{
		override def self: ReachContentWindowContext = this
		
		override def withTextContext(base: TextContext): ReachContentWindowContext =
			if (base == textContext) self else copy(textContext = base)
		override def withReachWindowContext(base: ReachWindowContext): ReachContentWindowContext =
			if (base == reachWindowContext) self else copy(reachWindowContext = base)
		
		override def *(mod: Double): ReachContentWindowContext = withContentContext(textContext * mod)
		
		override def withWindowBackground(bg: Color) = withBackground(bg)
		override def withBackground(background: Color) =
			copy(reachWindowContext.withWindowBackground(background), textContext.withBackground(background))
	}
}

/**
  * Common trait for context items that are used for creating Reach Popup windows
  * (that contain or are related with textual elements)
  * @author Mikko Hilpinen
  * @since 17.4.2023, v1.0
  */
trait ReachContentWindowContext extends ReachContentWindowContextLike[ReachContentWindowContext] with TextContext with ReachWindowContext
