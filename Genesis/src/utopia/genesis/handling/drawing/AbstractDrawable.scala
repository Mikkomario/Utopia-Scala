package utopia.genesis.handling.drawing

import utopia.genesis.graphics.DrawOrder

import scala.collection.mutable

/**
  * A partial Drawable implementation that manages the repaint listeners
  * @author Mikko Hilpinen
  * @since 25/02/2024, v4.0
  */
abstract class AbstractDrawable(override val drawOrder: DrawOrder = DrawOrder.default,
                                override val opaque: Boolean = false)
	extends Drawable
{
	// ATTRIBUTES   -----------------
	
	private val _repaintListeners = mutable.Set[RepaintListener]()
	
	
	// IMPLEMENTED  -----------------
	
	override def repaintListeners: Iterable[RepaintListener] = _repaintListeners
	
	override def addRepaintListener(listener: RepaintListener): Unit = _repaintListeners += listener
	override def removeRepaintListener(listener: RepaintListener): Unit = _repaintListeners -= listener
}
