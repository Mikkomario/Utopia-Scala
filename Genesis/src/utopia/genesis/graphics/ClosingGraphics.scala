package utopia.genesis.graphics

import utopia.flow.util.logging.SysErrLogger
import utopia.flow.view.immutable.eventful.Fixed
import utopia.flow.view.mutable.eventful.SettableFlag
import utopia.flow.view.template.Extender
import utopia.flow.view.template.eventful.Changing
import utopia.paradigm.transform.JavaAffineTransformConvertible

import java.awt.Graphics2D

object ClosingGraphics
{
	/**
	  * @param g A (root level) graphics instance to wrap
	  * @return A ClosingGraphics instance wrapping that graphics instance
	  */
	def apply(g: Graphics2D) = new ClosingGraphics(g, Fixed(false))
}

/**
  * Wraps a graphics instance and contains a future of the eventual closing event, also
  * @author Mikko Hilpinen
  * @since 15.5.2021, v2.5.1
  */
class ClosingGraphics(override val wrapped: Graphics2D, parentClosedFlag: => Changing[Boolean])
	extends AutoCloseable with Extender[Graphics2D]
{
	// ATTRIBUTES   --------------------------------
	
	private lazy val closedFlag = SettableFlag()(SysErrLogger)
	private lazy val statePointer = closedFlag || parentClosedFlag
	
	
	// COMPUTED ------------------------------------
	
	/**
	  * @return Future of the closing event of this graphics instance
	  */
	def closeFuture = statePointer.futureWhere { c => c }
	
	/**
	  * @return Whether this graphics instance has been closed already
	  */
	def isClosed = statePointer.value
	/**
	  * @return Whether this graphics instance is still open
	  */
	def isOpen = !isClosed
	
	
	// IMPLEMENTED  --------------------------------
	
	override def close() = {
		if (isOpen) {
			closedFlag.set()
			wrapped.dispose()
		}
	}
	
	
	// OTHER    -------------------------------------
	
	/**
	  * @return Creates a child graphics instance that is dependent from this one
	  */
	def createChild() = new ClosingGraphics(wrapped.create().asInstanceOf[Graphics2D], statePointer)
	
	/**
	  * Transforms this graphics instance using the specified affine transformation
	  * @param transformation New transformation to apply over the existing transformation
	  */
	def transform(transformation: JavaAffineTransformConvertible) =
		wrapped.transform(transformation.toJavaAffineTransform)
}
