package utopia.reach.context

import utopia.firmament.context.text.{StaticTextContext, VariableTextContext, VariableTextContextLike, VariableTextContextWrapper}

object VariableReachContentWindowContext
{
	// OTHER    -------------------------------
	
	/**
	  * @param windowContext Window context to wrap
	  * @param contentContext Context to use for creating the window contents
	  * @return A new window context
	  */
	def apply(windowContext: ReachWindowContext,
	          contentContext: VariableTextContext): VariableReachContentWindowContext =
		_Context(windowContext, contentContext)
	
	
	// NESTED   -------------------------------
	
	private case class _Context(windowContext: ReachWindowContext, base: VariableTextContext)
		extends VariableReachContentWindowContext
			with ReachWindowContextWrapper[ReachWindowContext, VariableReachContentWindowContext, StaticReachContentWindowContext]
			with VariableTextContextWrapper[VariableTextContext, VariableReachContentWindowContext]
	{
		override def self: VariableReachContentWindowContext = this
		override def current = StaticReachContentWindowContext(windowContext, base.current)
		override def toVariableContext = this
		
		override def actorHandler = super[VariableTextContextWrapper].actorHandler
		override def colorPointer = base.colorPointer
		
		override def withWindowContext(base: ReachWindowContext): VariableReachContentWindowContext =
			copy(windowContext = base)
		override def withBase(base: VariableTextContext): VariableReachContentWindowContext = copy(base = base)
		override def withContentContext(textContext: StaticTextContext) =
			StaticReachContentWindowContext(windowContext, textContext)
		
		override def *(mod: Double): VariableReachContentWindowContext = mapBase { _ * mod }
	}
}

/**
  * Common trait for variable context implementations which provide properties for creating Reach windows,
  * including their content
  * @author Mikko Hilpinen
  * @since 14.11.2024, v1.5
  */
trait VariableReachContentWindowContext
	extends VariableTextContext with VariableTextContextLike[VariableReachContentWindowContext]
		with ReachContentWindowContext
		with ReachContentWindowContextCopyable[VariableReachContentWindowContext, StaticReachContentWindowContext]
