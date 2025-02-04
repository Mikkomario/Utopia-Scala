package utopia.reach.focus

import utopia.firmament.context.ComponentCreationDefaults.componentLogger
import utopia.firmament.awt.AwtComponentExtensions._
import utopia.flow.collection.CollectionExtensions._
import utopia.flow.collection.immutable.{Empty, Pair, Single}
import utopia.flow.operator.ordering.CombinedOrdering
import utopia.flow.operator.sign.Sign
import utopia.flow.operator.sign.Sign.{Negative, Positive}
import utopia.flow.view.mutable.eventful.EventfulPointer
import utopia.paradigm.shape.shape2d.vector.point.Point
import utopia.reach.component.template.ReachComponent
import utopia.reach.component.template.focus.Focusable
import utopia.reach.focus.FocusEvent.{FocusEntering, FocusGained, FocusLeaving, FocusLost}

import java.awt.event.{FocusEvent, WindowAdapter, WindowEvent}
import scala.collection.immutable.HashMap

/**
  * This object manages focus traversal inside a reach canvas
  * @author Mikko Hilpinen
  * @since 21.10.2020, v0.1
  */
class ReachFocusManager(canvasComponent: java.awt.Component)
{
	// ATTRIBUTES	-----------------------------
	
	// TODO: This needs an update (sometimes components are a few pixels higher or lower)
	// Orders points from top to bottom, left to right
	private implicit val focusOrdering: Ordering[Point] =
		new CombinedOrdering[Point](Pair(Ordering.by[Point, Double] { _.y }, Ordering.by[Point, Double] { _.x }))
	
	private val targetsPointer = EventfulPointer(Set[Focusable]())
	private val orderedTargetsPointer = targetsPointer.lazyMap { targets =>
		sortComponents(targets.map { c => c.hierarchy.toVector -> c }.toVector) }
	private val targetIdsPointer = targetsPointer.lazyMap { _.map { _.focusId } }
	
	// Focus id -> Owned window
	private var windowOwnerships: Map[Int, java.awt.Window] = HashMap()
	// Owned window -> owner component
	private var reverseWindowOwnerships: Map[java.awt.Window, Focusable] = HashMap()
	private var currentOwnershipFocus: Option[(Focusable, java.awt.Window)] = None
	
	private var focusOwner: Option[Focusable] = None
	
	
	// INITIAL CODE	-----------------------------
	
	// Starts listening to focus events in the managed canvas
	canvasComponent.addFocusListener(CanvasFocusListener)
	
	
	// COMPUTED	---------------------------------
	
	/**
	  * @return Whether this focus manager / system currently owns the focus in the larger system
	  */
	def hasFocus = canvasComponent.isFocusOwner
	
	/**
	  * @return Current bounds of the component which owns the focus.
	  *         The resulting bounds are relative to the top-left corner of the screen.
	  */
	def absoluteFocusOwnerBounds = focusOwner.map { _.absoluteBounds }
	
	private def targets = targetsPointer.value
	private def targets_=(newTargets: Set[Focusable]) = targetsPointer.value = newTargets
	
	private def orderedTargets = orderedTargetsPointer.value
	
	private def targetIds = targetIdsPointer.value
	
	
	// OTHER	---------------------------------
	
	/**
	  * Registers a new focusable component to be managed
	  * @param component A component to be managed
	  */
	def register(component: Focusable) = {
		// If this manager is supposed to have focus but no component is currently the focus owner, attempts to make
		// this component the focus owner
		targets += component
		if (hasFocus && focusOwner.isEmpty && testFocusEnter(component))
			gainFocus(component)
	}
	
	/**
	  * Registers an ownership relationship between a component and a window. Whenever the specified window has
	  * focus, the component will also be considered to have focus. This relationship is automatically terminated when
	  * the window closes (is disposed).
	  * @param owner The component that owns the window
	  * @param window The window being owned. <b>Shouldn't contain this focus management system</b>.
	  */
	def registerWindowOwnership(owner: Focusable, window: java.awt.Window) = {
		windowOwnerships += owner.focusId -> window
		reverseWindowOwnerships += window -> owner
		window.addWindowFocusListener(OwnedWindowListener)
		window.addWindowListener(OwnedWindowListener)
		
		// Checks whether the window has focus and should share it with the new owner
		if (window.isFocused) {
			currentOwnershipFocus = Some(owner, window)
			focusOwner = Some(owner)
			owner.focusListeners.foreach { _.onFocusEvent(FocusGained) }
		}
	}
	
	/**
	  * Removes a component from the list of available focus targets
	  * @param component A component to remove
	  */
	def unregister(component: Focusable) = {
		// If the target component is the current focus owner, attempts to move the focus away first
		// (only if this manager has focus, otherwise simply informs the component about lost focus)
		if (isFocusOwner(component)) {
			if (hasFocus) {
				// If there weren't other components to receive the focus, simply removes the focus without a recipient
				if (!moveFocusInside(allowLooping = false, forceFocusLeave = true) &&
					!moveFocusInside(Negative, allowLooping = false, forceFocusLeave = true)) {
					component.focusListeners.foreach { _.onFocusEvent(FocusLost) }
					focusOwner = None
					// May yield focus from this whole system
					if (hasFocus)
						canvasComponent.transferFocus()
				}
			}
			else {
				component.focusListeners.foreach { _.onFocusEvent(FocusLost) }
				focusOwner = None
			}
		}
		// Removes the component from the list of available focus targets
		targets -= component
		windowOwnerships -= component.focusId
		reverseWindowOwnerships = reverseWindowOwnerships.filterNot { _._2.focusId == component.focusId }
		if (currentOwnershipFocus.exists { _._1.focusId == component.focusId })
			currentOwnershipFocus = None
	}
	
	/**
	  * Removes any ownership relation associated with the specified window
	  * @param window Window to no longer affect / share focus
	  */
	def removeOwnershipOf(window: java.awt.Window) = {
		windowOwnerships = windowOwnerships.filterNot { _._2 == window }
		reverseWindowOwnerships -= window
		// Informs focus lost if that window is the current focus target
		currentOwnershipFocus.filter { _._2 == window }.foreach { case (owner, _) =>
			currentOwnershipFocus = None
			owner.focusListeners.foreach { _.onFocusEvent(FocusLost) }
		}
		// Stops listening
		window.removeWindowFocusListener(OwnedWindowListener)
		window.removeWindowListener(OwnedWindowListener)
	}
	
	/**
	  * Tests whether a component is the current focus owner (in current system or whether it owns a focused window)
	  * @param component A component
	  * @return Whether that component is the current focus owner
	  */
	// Needs to have focus directly or be the owner of owned focused window
	def isFocusOwner(component: Focusable) = (focusOwner.exists { _.focusId == component.focusId } && hasFocus) ||
		currentOwnershipFocus.exists { _._1.focusId == component.focusId }
	
	/**
	  * Moves the focus one step forward or backward, keeping it always inside this component system
	  * @param direction Direction towards which the focus is moved (default = Positive = forward)
	  * @param allowLooping Whether focus should be moved back to the beginning or end of the list when reaching
	  *                     the other end (default = true)
	  * @param forceFocusLeave Whether to force the focus to leave the current component without testing its consent.
	  *                        If true, no FocusLeaving events will be generated. Default = false.
	  * @return True if new focus target was found and assigned or when current focus target denies focus movement.
	  *         False when a new focus target was not found or when this manager doesn't currently have focus to move.
	  */
	def moveFocusInside(direction: Sign = Positive, allowLooping: Boolean = true,
						forceFocusLeave: Boolean = false): Boolean =
	{
		if (hasFocus) {
			focusOwner match {
				// Case: One of the managed components currently has focus
				case Some(current) =>
					// Tests whether focus is allowed to leave (may be disabled)
					if (forceFocusLeave || testFocusLeave(current)) {
						// Finds the next focus target that allows focus entering
						val currentFocusId = current.focusId
						focusTargetsIterator(direction).dropWhile { _.focusId != currentFocusId }.drop(1)
							.find(testFocusEnter) match
						{
							// Case: Target found => Moves focus
							case Some(next) =>
								moveFocus(current, next)
								true
							// Case: No target found
							case None =>
								// May loop and search for previous components
								if (allowLooping)
									focusTargetsIterator(direction).takeWhile { _ != current }
										.find(testFocusEnter) match
									{
										case Some(next) =>
											moveFocus(current, next)
											true
										case None => false
									}
								else
									false
						}
					}
					else
						true
				// Case: None of the managed components currently has focus
				case None =>
					// Looks for any focus target that allows focus entering
					focusTargetsIterator(direction).find(testFocusEnter) match {
						// When a suitable target is found, makes it a focus owner
						case Some(next) =>
							gainFocus(next)
							true
						case None => false
					}
			}
		}
		else
			false
	}
	
	/**
	  * Moves the focus one step forward (or backward), possibly yielding it to another component / system entirely
	  * @param direction Direction towards which the focus is moved (default = Positive = forward)
	  * @param forceFocusLeave Whether to force the focus to leave the current component without testing its consent.
	  *                        If true, no FocusLeaving events will be generated. Default = false.
	  */
	def moveFocus(direction: Sign = Positive, forceFocusLeave: Boolean = false) = {
		// Checks whether there are other focusable components to target outside the managed focus system
		// If not, loops the focus inside the system without yielding it
		val isNextComponentAvailable = canYieldFocus(direction)
		val foundNext = moveFocusInside(direction, allowLooping = !isNextComponentAvailable, forceFocusLeave)
		// May transfer focus to the next component
		if (!foundNext && isNextComponentAvailable)
			direction match {
				case Positive => canvasComponent.transferFocus()
				case Negative => canvasComponent.transferFocusBackward()
			}
	}
	
	/**
	  * Moves the focus one step forward (or backward), possibly yielding it to another component / system entirely.
	  * Only moves the focus if the specified component is the current focus owner
	  * @param direction Direction towards which the focus is moved (default = Positive = forward)
	  * @param forceFocusLeave Whether to force the focus to leave the current component without testing its consent.
	  *                        If true, no FocusLeaving events will be generated. Default = false.
	  */
	def moveFocusFrom(component: Focusable, direction: Sign = Positive, forceFocusLeave: Boolean = false) = {
		if (isFocusOwner(component))
			moveFocus(direction, forceFocusLeave)
	}
	
	/**
	  * Moves the focus to the specified component
	  * @param newComponent A component to gain focus (must be one of the managed components)
	  * @param forceFocusLeave Whether the current focus owner should be forced to lose focus. If true, no FocusLeaving
	  *                        events are generated. Default = false.
	  * @param forceFocusEnter Whether the targeted component should be forced to accept focus. If true, no FocusEntering
	  *                        events are generated. Default = false.
	  * @return Whether focus was moved to the specified component. False if the current focus owner denied focus
	  *         leave or if the targeted component denied focus enter. Also returns false in cases where the targeted
	  *         component is not currently managed by this focus manager or when this manager is unable to gain focus.
	  */
	def moveFocusTo(newComponent: Focusable, forceFocusLeave: Boolean = false, forceFocusEnter: Boolean = false): Boolean =
	{
		// Skips the process if the target component already has focus
		if (isFocusOwner(newComponent))
			true
		else {
			// Targeted component must be registered
			if (targetIds.contains(newComponent.focusId)) {
				if (hasFocus) {
					// Tests focus leaving from current focus owner (optional)
					if (forceFocusLeave || focusOwner.forall(testFocusLeave)) {
						// Tests focus enter to new component (optional)
						if (forceFocusEnter || testFocusEnter(newComponent)) {
							// Moves the focus
							focusOwner match {
								case Some(current) => moveFocus(current, newComponent)
								case None => gainFocus(newComponent)
							}
							true
						}
						else
							false
					}
					else
						false
				}
				// If this manager doesn't currently have focus, attempts to gain it
				else if (forceFocusEnter || testFocusEnter(newComponent)) {
					focusOwner = Some(newComponent)
					canvasComponent.requestFocusInWindow()
				}
				else
					false
			}
			else
				false
		}
	}
	
	/**
	  * @param direction A direction towards which the focus would be transferred (default = Positive = forward)
	  * @return Whether this system is able to yield the focus to that direction (false if the managed canvas is the
	  *         only focusable component in its cycle)
	  */
	def canYieldFocus(direction: Sign = Positive) = {
		val focusCycleRoot = canvasComponent.getFocusCycleRootAncestor
		val focusPolicy = focusCycleRoot.getFocusTraversalPolicy
		// Checks whether there are other focusable components to target outside the managed focus system
		// If not, loops the focus inside the system without yielding it
		val nextComp = direction match {
			case Positive => focusPolicy.getComponentAfter(focusCycleRoot, canvasComponent)
			case Negative => focusPolicy.getComponentBefore(focusCycleRoot, canvasComponent)
		}
		nextComp != null && nextComp != canvasComponent
	}
	
	private def moveFocus(from: Focusable, to: Focusable) = {
		from.focusListeners.foreach { _.onFocusEvent(FocusLost) }
		gainFocus(to)
	}
	
	private def gainFocus(target: Focusable) = {
		focusOwner = Some(target)
		target.focusListeners.foreach { _.onFocusEvent(FocusGained) }
	}
	
	private def testFocusEnter(target: Focusable) = {
		target.focusListeners.foreach { _.onFocusEvent(FocusEntering) }
		target.allowsFocusEnter
	}
	
	private def testFocusLeave(target: Focusable) = {
		target.focusListeners.foreach { _.onFocusEvent(FocusLeaving) }
		target.allowsFocusLeave
	}
	
	private def focusTargetsIterator(direction: Sign) = direction match {
		case Positive => orderedTargets.iterator
		case Negative => orderedTargets.reverseIterator
	}
	
	// Traverses same container components in sequence before jumping to the container's sibling
	private def sortComponents(components: Vector[(Vector[ReachComponent], Focusable)]): Vector[Focusable] = {
		val (directTargets, deepTargets) = components.divideWith { case (hierarchy, target) =>
			if (hierarchy.isEmpty)
				Left(target)
			else
				Right(hierarchy.head -> (hierarchy.tail, target))
		}
		val groupedDeepTargets = deepTargets.groupMap { _._1 } { _._2 }
		
		(directTargets.map { c => c.position -> Single(c) } ++
			groupedDeepTargets.map { case (c, targets) => c.position -> sortComponents(targets) })
			.sortBy { _._1 }.flatMap { _._2 }
	}
	
	
	// NESTED	---------------------------------------
	
	private object CanvasFocusListener extends java.awt.event.FocusListener
	{
		// IMPLEMENTED	-------------------------------
		
		override def focusGained(e: FocusEvent) =
		{
			// Checks focus traversal direction (unfortunately there is no direct method call available for this)
			val direction = if (e.paramString().contains("TRAVERSAL_BACKWARD")) Negative else Positive
			// Informs the current focus owner or finds a new one
			focusOwner match {
				case Some(current) =>
					// If the focus owner owned the previous focus system, doesn't generate a new focus gained event
					// TODO: Another kind of event may be generated, however
					if (!componentOwnsOppositeIn(current, e))
						current.focusListeners.foreach { _.onFocusEvent(FocusGained) }
				case None =>
					focusTargetsIterator(direction).find(testFocusEnter) match {
						case Some(next) => gainFocus(next)
						case None =>
							// Yields the focus forward if there was no managed component that could take it
							if (canYieldFocus(direction)) {
								direction match {
									case Positive => canvasComponent.transferFocus()
									case Negative => canvasComponent.transferFocusBackward()
								}
							}
					}
			}
		}
		
		override def focusLost(e: FocusEvent) = {
			// Informs the current focus owner of focus lost, unless the current focus owner also owns the
			// window the focus was moved to
			focusOwner.foreach { owner =>
				if (!componentOwnsOppositeIn(owner, e))
					owner.focusListeners.foreach { _.onFocusEvent(FocusLost) }
			}
		}
		
		
		// OTHER	----------------------------------
		
		private def componentOwnsOppositeIn(component: Focusable, event: FocusEvent) =
			windowOwnerships.get(component.focusId).exists { owned => windowsFrom(event).contains(owned) }
		
		private def windowsFrom(event: FocusEvent) = Option(event.getOppositeComponent) match {
			case Some(component) => component.parentWindowsIterator.toVector
			case None => Empty
		}
	}
	
	private object OwnedWindowListener extends WindowAdapter
	{
		// COMPUTED	---------------------------------
		
		private def managedWindow = canvasComponent.parentWindow
		
		
		// IMPLEMENTED	-----------------------------
		
		override def windowGainedFocus(e: WindowEvent) =
		{
			// Registers the current window ownership, if present
			val newFocusWindow = e.getWindow
			reverseWindowOwnerships.get(newFocusWindow).foreach { owner =>
				currentOwnershipFocus = Some(owner -> newFocusWindow)
				val previousFocusOwner = focusOwner
				focusOwner = Some(owner)
				def informNewFocusOwner() = owner.focusListeners.foreach { _.onFocusEvent(FocusGained) }
				// May inform the focus owner, if it wasn't already in focus
				Option(e.getOppositeWindow) match {
					case Some(oldFocusWindow) =>
						reverseWindowOwnerships.get(oldFocusWindow) match {
							case Some(previousOwner) =>
								// Expects the previous focus owner to already be informed about the focus lost event
								if (previousOwner.focusId != owner.focusId)
									informNewFocusOwner()
							case None =>
								// Checks if focus moved away from managed component
								if (isManagedWindow(oldFocusWindow) && canvasIsOrWasFocusedIn(oldFocusWindow)) {
									if (previousFocusOwner.forall { _.focusId != owner.focusId })
										informNewFocusOwner()
								}
								else
									informNewFocusOwner()
						}
					case None => informNewFocusOwner()
				}
			}
		}
		
		override def windowLostFocus(e: WindowEvent) = {
			val oldFocusWindow = e.getWindow
			currentOwnershipFocus.foreach { case (owner, ownedWindow) =>
				// Makes sure the owned window is up to date
				if (ownedWindow == oldFocusWindow) {
					// Informs the owner about the focus lost event, except if it will own the new window or be the
					// targeted component in this (managed) window
					def informFocusLost() = owner.focusListeners.foreach { _.onFocusEvent(FocusLost) }
					Option(e.getOppositeWindow) match {
						case Some(newFocusWindow) =>
							reverseWindowOwnerships.get(newFocusWindow) match {
								// Case: Focus will move to another owned window
								case Some(newOwner) => if (newOwner.focusId != owner.focusId) informFocusLost()
								case None =>
									// Checks whether focus will move to this (managed) window and managed canvas
									if (isManagedWindow(newFocusWindow) && canvasIsOrWasFocusedIn(newFocusWindow)) {
										// Fires focus lost event if the focus won't be given directly to
										// that same component
										if (focusOwner.forall { _.focusId != owner.focusId })
											informFocusLost()
									}
									else
										informFocusLost()
							}
						// Case: Focus will move outside Java
						case None => informFocusLost()
					}
				}
			}
			currentOwnershipFocus = None
		}
		
		override def windowClosing(e: WindowEvent) = removeOwnershipOf(e.getWindow)
		override def windowClosed(e: WindowEvent) = removeOwnershipOf(e.getWindow)
		
		
		// OTHER	--------------------------
		
		private def isManagedWindow(window: java.awt.Window) = managedWindow.contains(window)
		
		private def canvasIsOrWasFocusedIn(window: java.awt.Window) =
			Option(window.getFocusOwner).orElse(Option(window.getMostRecentFocusOwner)).contains(canvasComponent)
	}
}
