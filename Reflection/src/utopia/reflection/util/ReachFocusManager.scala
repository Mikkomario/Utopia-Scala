package utopia.reflection.util

import utopia.flow.datastructure.mutable.PointerWithEvents
import utopia.flow.util.CollectionExtensions._
import utopia.flow.util.CombinedOrdering
import utopia.genesis.shape.shape1D.Direction1D
import utopia.genesis.shape.shape1D.Direction1D.{Negative, Positive}
import utopia.genesis.shape.shape2D.Point
import utopia.reflection.component.reach.template.{Focusable, ReachComponentLike}
import utopia.reflection.event.FocusEvent.{FocusEntering, FocusGained, FocusLeaving, FocusLost}

/**
  * This object manages focus traversal inside a reach canvas
  * @author Mikko Hilpinen
  * @since 21.10.2020, v2
  */
class ReachFocusManager
{
	// ATTRIBUTES	-----------------------------
	
	private val targetsPointer = new PointerWithEvents(Set[Focusable]())
	private val orderedTargetsPointer = targetsPointer.lazyMap { targets =>
		sortComponents(targets.map { c => c.parentHierarchy.toVector -> c }.toVector) }
	
	private var focusOwner: Option[Focusable] = None
	
	// Orders points from top to bottom, left to right
	private implicit val focusOrdering: Ordering[Point] =
		new CombinedOrdering[Point](Vector(Ordering.by { _.y }, Ordering.by { _.x }))
	
	
	// COMPUTED	---------------------------------
	
	def hasFocus = focusOwner.nonEmpty
	
	private def targets = targetsPointer.value
	private def targets_=(newTargets: Set[Focusable]) = targetsPointer.value = newTargets
	
	private def orderedTargets = orderedTargetsPointer.get
	
	
	// OTHER	---------------------------------
	
	/**
	  * Registers a new focusable component to be managed
	  * @param component A component to be managed
	  */
	def register(component: Focusable) = targets += component
	
	/**
	  * Removes a component from the list of available focus targets
	  * @param component A component to remove
	  */
	def unregister(component: Focusable) =
	{
		// If the target component is the current focus owner, attempts to move the focus away first
		if (isFocusOwner(component))
		{
			// If there weren't other components to receive the focus, simply removes the focus without a recipient
			if (!moveFocusInside(allowLooping = false, forceFocusLeave = true) &&
				!moveFocusInside(Negative, allowLooping = false, forceFocusLeave = true))
			{
				component.focusListeners.foreach { _.onFocusEvent(FocusLost) }
				focusOwner = None
			}
		}
		// Removes the component from the list of available focus targets
		targets -= component
	}
	
	/**
	  * Tests whether a component is the current focus owner
	  * @param component A component
	  * @return Whether that component is the current focus owner
	  */
	def isFocusOwner(component: Any) = focusOwner.contains(component)
	
	/**
	  * Moves the focus one step forward or backward
	  * @param direction Direction towards which the focus is moved (default = Positive = forward)
	  * @param allowLooping Whether focus should be moved back to the beginning or end of the list when reaching
	  *                     the other end (default = true)
	  * @param forceFocusLeave Whether to force the focus to leave the current component without testing its consent.
	  *                        If true, no FocusLeaving events will be generated. Default = false.
	  * @return True if new focus target was found and assigned or when current focus target denies focus movement.
	  *         False when a new focus target was not found.
	  */
	def moveFocusInside(direction: Direction1D = Positive, allowLooping: Boolean = true, forceFocusLeave: Boolean = false) =
	{
		focusOwner match
		{
			// Case: One of the managed components currently has focus
			case Some(current) =>
				// Tests whether focus is allowed to leave (may be disabled)
				if (forceFocusLeave || testFocusLeave(current))
				{
					// Finds the next focus target that allows focus entering
					focusTargetsIterator(direction).dropWhile { _ != current }.drop(1).find(testFocusEnter) match
					{
						// Case: Target found => Moves focus
						case Some(next) =>
							moveFocus(current, next)
							true
						// Case: No target found
						case None =>
							// May loop and search for previous components
							if (allowLooping)
								focusTargetsIterator(direction).takeWhile { _ != current }.find(testFocusEnter) match
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
				focusTargetsIterator(direction).find(testFocusEnter) match
				{
					// When a suitable target is found, makes it a focus owner
					case Some(next) =>
						gainFocus(next)
						true
					case None => false
				}
		}
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
	  *         component is not currently managed by this focus manager.
	  */
	def moveFocusTo(newComponent: Focusable, forceFocusLeave: Boolean = false, forceFocusEnter: Boolean = false) =
	{
		if (targets.contains(newComponent) && (forceFocusLeave || focusOwner.forall(testFocusLeave)))
		{
			if (forceFocusEnter || testFocusEnter(newComponent))
			{
				focusOwner match
				{
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
	
	private def moveFocus(from: Focusable, to: Focusable) =
	{
		from.focusListeners.foreach { _.onFocusEvent(FocusLost) }
		gainFocus(to)
	}
	
	private def gainFocus(target: Focusable) =
	{
		focusOwner = Some(target)
		target.focusListeners.foreach { _.onFocusEvent(FocusGained) }
	}
	
	private def testFocusEnter(target: Focusable) =
	{
		target.focusListeners.foreach { _.onFocusEvent(FocusEntering) }
		target.allowsFocusEnter
	}
	
	private def testFocusLeave(target: Focusable) =
	{
		target.focusListeners.foreach { _.onFocusEvent(FocusLeaving) }
		target.allowsFocusLeave
	}
	
	private def focusTargetsIterator(direction: Direction1D) = direction match
	{
		case Positive => orderedTargets.iterator
		case Negative => orderedTargets.reverseIterator
	}
	
	private def sortComponents(components: Vector[(Vector[ReachComponentLike], Focusable)]): Vector[Focusable] =
	{
		val (directTargets, deepTargets) = components.dividedWith { case (hierarchy, target) =>
			if (hierarchy.isEmpty)
				Left(target)
			else
				Right(hierarchy.head -> (hierarchy.tail, target))
		}
		val groupedDeepTargets = deepTargets.asMultiMap
		
		(directTargets.map { c => c.position -> Vector(c) } ++
			groupedDeepTargets.map { case (c, targets) => c.position -> sortComponents(targets) })
			.sortBy { _._1 }.flatMap { _._2 }
	}
}
