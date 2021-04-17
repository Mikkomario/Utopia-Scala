package utopia.reach.component.template

import utopia.genesis.handling.mutable.ActorHandler
import utopia.genesis.shape.shape1D.Direction1D
import utopia.genesis.shape.shape1D.Direction1D.Positive
import utopia.reach.component.hierarchy.ComponentHierarchy
import utopia.reach.component.wrapper.ComponentCreationResult
import utopia.reach.focus.{FocusListener, FocusRequestable}
import utopia.reflection.container.swing.window.Popup.PopupAutoCloseLogic
import utopia.reflection.container.swing.window.Popup.PopupAutoCloseLogic.Never
import utopia.reflection.shape.Alignment

object Focusable
{
	// OTHER	--------------------------------
	
	/**
	  * Wraps a component as a focusable item
	  * @param component A component to wrap
	  * @param focusListeners Focus listeners to assign to this component
	  * @return The wrapped component
	  */
	def wrap[C <: ReachComponentLike](component: C, focusListeners: Seq[FocusListener]) =
		new FocusWrapper(component, focusListeners)
	
	
	// NESTED	--------------------------------
	
	/**
	  * Used for wrapping another non-focusable component to produce a focusable component
	  * @param wrapped Component to wrap
	  * @param focusListeners Focus listeners to assign to the component
	  */
	class FocusWrapper[+C <: ReachComponentLike](override val wrapped: C, override val focusListeners: Seq[FocusListener])
		extends Focusable with ReachComponentWrapper
	{
		// INITIAL CODE	------------------------
		
		// Enables focus management while attached to the main hierarchy
		enableFocusHandlingWhileLinked()
		
		
		// IMPLEMENTED	------------------------
		
		override def focusId = hashCode()
		
		override def allowsFocusEnter = true
		
		override def allowsFocusLeave = true
	}
}

/**
  * A common trait for focusable (reach) components
  * @author Mikko Hilpinen
  * @since 21.10.2020, v0.1
  */
trait Focusable extends ReachComponentLike with FocusRequestable
{
	// ABSTRACT	--------------------------------
	
	/**
	  * @return Unique identifier for this focusable component
	  */
	def focusId: Int
	
	/**
	  * @return Listeners that will be informed of this component's focus changes
	  */
	def focusListeners: Seq[FocusListener]
	
	/**
	  * @return Whether this component currently allows focus gain
	  */
	def allowsFocusEnter: Boolean
	
	/**
	  * @return Whether this component currently allows focus leave
	  */
	def allowsFocusLeave: Boolean
	
	
	// COMPUTED	-------------------------------
	
	/**
	  * @return The focus manager associated with this component
	  */
	def focusManager = parentCanvas.focusManager
	
	
	// IMPLEMENTED	---------------------------
	
	/**
	  * Requests a focus gain for this component
	  * @param forceFocusLeave Whether focus should be forced to leave from the current focus owner (default = false)
	  * @param forceFocusEnter Whether focus should be forced to enter this component (default = false)
	  * @return Whether this component received (or is likely to receive) focus
	  */
	override def requestFocus(forceFocusLeave: Boolean = false, forceFocusEnter: Boolean = false) =
		focusManager.moveFocusTo(this, forceFocusLeave, forceFocusEnter)
	
	
	// OTHER	-------------------------------
	
	/**
	  * Registers this component to the focus manager
	  */
	protected def enableFocusHandling() = focusManager.register(this)
	
	/**
	  * Detaches this component from the focus manager
	  */
	protected def disableFocusHandling() = focusManager.unregister(this)
	
	/**
	  * Connects this component to the focus manager while linked to the main component hierarchy. Detaches from
	  * the focus manager while not linked.
	  */
	protected def enableFocusHandlingWhileLinked() =
	{
		// Updates registration based on link status
		addHierarchyListener { isLinked =>
			if (isLinked)
				enableFocusHandling()
			else
				disableFocusHandling()
		}
		// Performs the initial registration if already linked
		if (parentHierarchy.isLinked)
			enableFocusHandling()
	}
	
	/**
	  * Moves the focus one step forward (or backward) from this component.
	  * Only moves the focus if this component is the current focus owner
	  * @param direction Direction towards which the focus is moved (default = Positive = forward)
	  * @param forceFocusLeave Whether to force the focus to leave this component without testing its consent.
	  *                        If true, no FocusLeaving events will be generated. Default = false.
	  */
	def yieldFocus(direction: Direction1D = Positive, forceFocusLeave: Boolean = false) =
		focusManager.moveFocusFrom(this, direction, forceFocusLeave)
	
	/**
	  * Informs the focus management system that this component now owns the specified window. While the specified
	  * window has focus, this component will also be treated as having focus. The ownership relationship will
	  * automatically terminate when the window closes.
	  * @param window The window this component will own
	  */
	def registerOwnershipOf(window: java.awt.Window) = focusManager.registerWindowOwnership(this, window)
	
	/**
	  * Creates a pop-up next to this component. Registers this component as the owner of that pop-up window
	  * @param actorHandler Actor handler that will deliver action events for the pop-up
	  * @param alignment Alignment to use when placing the pop-up (default = Right)
	  * @param margin Margin to place between this component and the pop-up (not used with Center alignment)
	  * @param autoCloseLogic Logic used for closing the pop-up (default = won't automatically close the pop-up)
	  * @param makeContent A function for producing pop-up contents based on a component hierarchy
	  * @tparam C Type of created component
	  * @tparam R Type of additional result
	  * @return A component wrapping result that contains the pop-up, the created component inside the canvas and
	  *         the additional result returned by 'makeContent'
	  */
	def createOwnedPopup[C <: ReachComponentLike, R](actorHandler: ActorHandler, alignment: Alignment = Alignment.Right,
													 margin: Double = 0.0, autoCloseLogic: PopupAutoCloseLogic = Never)
													(makeContent: ComponentHierarchy => ComponentCreationResult[C, R]) =
	{
		val popup = createPopup[C, R](actorHandler, alignment, margin, autoCloseLogic)(makeContent)
		registerOwnershipOf(popup.component)
		popup
	}
}
