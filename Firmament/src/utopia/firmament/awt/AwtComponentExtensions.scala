package utopia.firmament.awt

import scala.language.implicitConversions

/**
  * Contains some extensions for awt and swing components
  * @author Mikko Hilpinen
  * @since 13.9.2020, Reflection v1.3
  */
object AwtComponentExtensions
{
	implicit class RichAwtComponent(val c: java.awt.Component) extends AnyVal
	{
		/**
		  * @return An iterator that first returns the parent component of this component, then the parent component
		  *         of that component and so on, as long as there are parent components available
		  */
		def parents = Iterator
			.iterate(Option(c.getParent)) { _.flatMap { c => Option(c.getParent) } }
			.takeWhile { _.isDefined }.map { _.get }
		
		/**
		  * @return Whether this component is in a visible component hierarchy
		  */
		// Iterates over parents, seeking the first window instance. Stops if getParent returns null
		def isInWindow = parents.exists { _.isInstanceOf[java.awt.Window] }
		
		/**
		  * @return The first window that contains this component. None if this component isn't attached to any window
		  */
		def parentWindow = parents.find { _.isInstanceOf[java.awt.Window] }
			.map { _.asInstanceOf[java.awt.Window] }
		
		/**
		  * @return An iterator that returns the series of windows this component is attached to, with the closest
		  *         window returned first and the owner of that window returned after that. If this component is
		  *         not attached to any window, an empty iterator is returned.
		  */
		def parentWindows = parents.filter { _.isInstanceOf[java.awt.Window] }
			.map { _.asInstanceOf[java.awt.Window] }
		
		/**
		  * @return Whether this component is part of a visible component hierarchy (that includes a visible window)
		  */
		def isInVisibleHierarchy = c.isVisible &&
			parents.takeWhile { _.isVisible }.exists { _.isInstanceOf[java.awt.Window] }
	}
}
