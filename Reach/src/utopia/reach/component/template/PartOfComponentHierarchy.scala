package utopia.reach.component.template

import utopia.firmament.context.HasContext
import utopia.firmament.context.base.BaseContextPropsView
import utopia.reach.component.hierarchy.ComponentHierarchy
import utopia.reach.container.ReachCanvas

object PartOfComponentHierarchy
{
	// EXTENSIONS   ------------------------
	
	implicit class ContextualPartOfHierarchy(val p: PartOfComponentHierarchy with HasContext[BaseContextPropsView])
	{
		/**
		 * @return Font metrics to use under the current (font) settings.
		 *         Note: These only represent the current state. If the font is variable, this value won't reflect it.
		 * @see [[fontMetricsPointer]]
		 */
		def fontMetrics = p.hierarchy.fontMetricsWith(p.context.fontPointer.value)
		/**
		 * @return A pointer that contains the font metrics to use within this context.
		 *         Applies the contextual font.
		 */
		def fontMetricsPointer =
			p.context.fontPointer.mapWhile(p.hierarchy.linkedFlag) { font =>
				p.hierarchy.fontMetricsWith(font)
			}
	}
}

/**
  * Common trait for components and other elements that specify a parent component hierarchy.
  * @author Mikko Hilpinen
  * @since 08/01/2024, v1.2
  */
trait PartOfComponentHierarchy
{
	// ABSTRACT ---------------------------
	
	/**
	  * @return The component hierarchy this component is part of.
	  *         Returns the hierarchy block that matches the direct parent of this component.
	  */
	def hierarchy: ComponentHierarchy
	
	
	// COMPUTED --------------------------
	
	/**
	  * @return The Reach Canvas -element to which this component belongs
	  */
	implicit def canvas: ReachCanvas = hierarchy.top
	
	/**
	  * @return A flag that contains true while this component is linked to the main component hierarchy
	  */
	def linkedFlag = hierarchy.linkedFlag
	
	/**
	 * @return Whether this component is linked to the main component hierarchy
	 */
	def isLinked = linkedFlag.value
	/**
	 * @return Whether this component is not currently linked to the main component hierarchy
	 */
	def isDetached = !isLinked
	
	@deprecated("Deprecated for removal. Please use .hierarchy instead", "v1.6")
	def parentHierarchy: ComponentHierarchy = hierarchy
}
