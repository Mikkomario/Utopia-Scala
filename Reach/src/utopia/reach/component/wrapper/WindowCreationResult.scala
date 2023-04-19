package utopia.reach.component.wrapper

import utopia.firmament.component.Window
import utopia.reach.container.ReachCanvas

import scala.language.implicitConversions

object WindowCreationResult
{
	// IMPLICIT --------------------
	
	implicit def accessParent(r: WindowCreationResult[_, _]): Window = r.window
	
	
	// OTHER    -------------------
	
	/**
	  * Wraps a canvas creation result
	  * @param window A window that hosts the canvas
	  * @param canvasCreation Canvas creation result
	  * @tparam C Type of canvas content
	  * @tparam R Type of additional result
	  * @return A new window creation result
	  */
	def apply[C, R](window: Window, canvasCreation: ComponentWrapResult[ReachCanvas, C, R]): WindowCreationResult[C, R] =
		apply(window, canvasCreation.parent, canvasCreation.child, canvasCreation.result)
	
	/**
	  * Wraps the created components
	  * @param window Created window
	  * @param canvas Created canvas
	  * @param content Created canvas content
	  * @tparam C Type of created content
	  * @return A new window creation result with no additional result
	  */
	def apply[C](window: Window, canvas: ReachCanvas, content: C): WindowCreationResult[C, Unit] =
		apply[C, Unit](window, canvas, content, ())
}

/**
  * A result which contains components that were created during a window creation process, namely:
  *     - The created window
  *     - The created canvas element
  *     - The created canvas content
  * Also provides one slot for additional information
  *
  * @author Mikko Hilpinen
  * @since 14.4.2023, v1.0
  */
case class WindowCreationResult[+C, +R](window: Window, canvas: ReachCanvas, content: C, result: R)
{
	// COMPUTED -----------------------
	
	/**
	  * @return Window + Canvas + Content
	  */
	def toTriple = (window, canvas, content)
	/**
	  * @return Window + Canvas + Content + Result
	  */
	def toQuadruple = (window, canvas, content, result)
	
	/**
	  * @return Window + Result
	  */
	def windowAndResult = (window, result)
	/**
	  * @return Window + Canvas
	  */
	def windowAndCanvas = (window, canvas)
	/**
	  * @return Window + Content
	  */
	def windowAndContent = (window, content)
	
	
	// OTHER    -----------------------
	
	/**
	  * @param result A new result to attach
	  * @tparam R2 Type of the new result
	  * @return A copy of this creation result with the specified additional result
	  */
	def withResult[R2](result: R2) = copy(result = result)
	/**
	  * @param f A mapping function for the additional result
	  * @tparam R2 Mapping function return type
	  * @return A copy of this result with mapped additional result
	  */
	def mapResult[R2](f: R => R2) = withResult(f(result))
}
