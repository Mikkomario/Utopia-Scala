package utopia.reach.component.factory

import utopia.flow.util.Mutate
import utopia.reach.focus.FocusListener

/**
  * Common trait for component factories that allow the assigning of focus listeners
  * @author Mikko Hilpinen
  * @since 18.5.2023, v1.1
  */
trait FocusListenableFactory[+Repr]
{
	// ABSTRACT ----------------------------
	
	/**
	  * @return Currently attached focus listeners
	  */
	protected def focusListeners: Seq[FocusListener]
	/**
	  * @param listeners Focus listeners to attach to this component (exclusive)
	  * @return Copy of this factory with the specified focus listeners (only)
	  */
	def withFocusListeners(listeners: Seq[FocusListener]): Repr
	
	
	// OTHER    ----------------------------
	
	/**
	  * @param f A mapping function used to modify the focus listeners attached to this factory
	  * @return Copy of this factory with modified listeners
	  */
	def mapFocusListeners(f: Mutate[Seq[FocusListener]]) = withFocusListeners(f(focusListeners))
	
	/**
	  * @param listeners Focus listeners to assign
	  * @return Copy of this factory that includes the specified focus listeners
	  */
	def withAdditionalFocusListeners(listeners: IterableOnce[FocusListener]) = mapFocusListeners { _ ++ listeners }
	/**
	  * @param listener Focus listener to add to this factory
	  * @return Copy of this factory that includes the specified focus listener
	  */
	def withFocusListener(listener: FocusListener) = mapFocusListeners { _ :+ listener }
}
