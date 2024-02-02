package utopia.manuscript.excel

import utopia.flow.generic.model.immutable.Value
import utopia.flow.view.immutable.View
import utopia.flow.view.immutable.caching.Lazy
import utopia.paradigm.shape.template.Dimensions

/**
  * Represents an individual cell in a spread-sheet
  * @author Mikko Hilpinen
  * @since 31/01/2024, v1.0
  */
class Cell(val index: Dimensions[Int], val lazyValue: Lazy[Value]) extends View[Value]
{
	// COMPUTED ---------------------------
	
	/**
	  * @return The index of this cell's column (0-based)
	  */
	def x = index.x
	/**
	  * @return The index of this cell's column (0-based)
	  */
	def y = index.y
	
	
	// IMPLEMENTED  -----------------------
	
	override def value: Value = lazyValue.value
	
	override def valueIterator = lazyValue.valueIterator
	override def mapValue[B](f: Value => B) = lazyValue.mapValue(f)
}
