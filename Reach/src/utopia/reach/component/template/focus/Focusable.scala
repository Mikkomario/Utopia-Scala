package utopia.reach.component.template.focus

import utopia.firmament.localization.LocalizedString
import utopia.flow.operator.sign.Sign
import utopia.flow.operator.sign.Sign.Positive
import utopia.flow.util.logging.Logger
import utopia.paradigm.enumeration.Alignment
import utopia.reach.component.hierarchy.ComponentHierarchy
import utopia.reach.component.template.{ReachComponentLike, ReachComponentWrapper}
import utopia.reach.component.wrapper.{ComponentCreationResult, WindowCreationResult}
import utopia.reach.context.ReachWindowContext
import utopia.reach.focus.{FocusListener, FocusRequestable}

import scala.concurrent.ExecutionContext

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
	protected def enableFocusHandlingWhileLinked() = {
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
	def yieldFocus(direction: Sign = Positive, forceFocusLeave: Boolean = false) =
		focusManager.moveFocusFrom(this, direction, forceFocusLeave)
	
	/**
	  * Informs the focus management system that this component now owns the specified window. While the specified
	  * window has focus, this component will also be treated as having focus. The ownership relationship will
	  * automatically terminate when the window closes.
	  * @param window The window this component will own
	  */
	def registerOwnershipOf(window: java.awt.Window) = focusManager.registerWindowOwnership(this, window)
	/**
	  * Creates a new window next to this component.
	  * Registers this component as the focus owner of the created window.
	  * This means that this component is considered to have focus as long as the specified window has focus.
	  *
	  * @param alignment     Alignment used when positioning the window relative to this component.
	  *                      E.g. If Center is used, will position the over the center of this component.
	  *                      Or if Right is used, will position this window right of this component.
	  *
	  *                      Please note that this alignment may be reversed in case there is not enough space
	  *                      on that side.
	  *
	  *                      Bi-directional alignments, such as TopLeft will place the window next to the component
	  *                      diagonally (so that they won't share any edge together).
	  *
	  *                      Default = Right
	  *
	  * @param margin        Margin placed between this component and the window, when possible
	  *                      (ignored if preferredAlignment=Center).
	  *                      Default = 0
	  * @param title         Title displayed on the window (provided that OS headers are in use).
	  *                      Default = empty = no title.
	  * @param matchEdgeLength Whether the window should share an edge length with the anchor component.
	  *                        E.g. If bottom alignment is used and 'matchEdgeLength' is enabled, the resulting
	  *                        window will attempt to stretch so that to matches the width of the 'component'.
	  *                        The stacksize limits of the window will be respected, however, and may limit the
	  *                        resizing.
	  *                        Default = false = will not resize the window.
	  * @param keepAnchored  Whether the window should be kept close to this component when its size changes
	  *                      or the this component is moved or resized.
	  *                      Set to false if you don't expect the owner component to move.
	  *                      This will save some resources, as a large number of components needs to be tracked.
	  *                      Default = true.
	  * @param display       Whether the window should be displayed immediately (default = false)
	  * @param createContent A function that accepts a component hierarchy and creates the canvas content.
	  *                      May return an additional result, that will be included in the result of this function.
	  * @param context       Implicit window creation context
	  * @param exc           Implicit execution context
	  * @param log           Implicit logging implementation
	  * @tparam C Type of created canvas content
	  * @tparam R Type of additional function result
	  * @return A new window + created canvas + created canvas content + additional creation result
	  */
	def createOwnedWindow[C <: ReachComponentLike, R](alignment: Alignment = Alignment.Right, margin: Double = 0.0,
	                                                  title: LocalizedString = LocalizedString.empty,
	                                                  matchEdgeLength: Boolean = false, keepAnchored: Boolean = true,
	                                                  display: Boolean = false)
	                                                 (createContent: ComponentHierarchy => ComponentCreationResult[C, R])
	                                                 (implicit context: ReachWindowContext, exc: ExecutionContext,
	                                                  log: Logger): WindowCreationResult[C, R] =
	{
		// Always enables focus on the created windows
		val window = createWindow[C, R](alignment, margin, title, matchEdgeLength, keepAnchored,
			display)(createContent)(context.focusable, exc, log)
		registerOwnershipOf(window.component)
		window
	}
}
