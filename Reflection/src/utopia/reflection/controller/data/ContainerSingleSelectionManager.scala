package utopia.reflection.controller.data

import utopia.flow.datastructure.mutable.PointerWithEvents
import utopia.flow.event.ChangingLike
import utopia.flow.operator.EqualsFunction
import utopia.reflection.component.template.ComponentLike2
import utopia.reflection.component.template.display.Refreshable
import utopia.reflection.container.template.mutable.MutableMultiContainer2

object ContainerSingleSelectionManager
{
	/**
	  * Short version of typical pointer used in these methods
	  */
	private type P[X] = ChangingLike[Vector[X]]
	
	/**
	  * Short version for a refreshable display component
	  */
	private type D[X] = ComponentLike2 with Refreshable[X]
	
	/**
	  * Creates a selection manager for immutable items that don't represent state of any other object. No two different
	  * items will be linked in any way.
	  * @param container Container that will hold the displays
	  * @param contentPointer Pointer to the displayed content
	  * @param valuePointer A pointer that contains the currently selected value (if any)
	  * @param equalsCheck Function for checking item equality (default = standard equals (== -operator))
	  * @param makeDisplay Function for creating new displays
	  * @tparam A Type of displayed item
	  * @tparam Display Type of display component
	  * @return New content displayer
	  */
	def forStatelessItems[A, W, Display <: D[A]](container: MutableMultiContainer2[W, Display],
												 contentPointer: P[A],
												 valuePointer: PointerWithEvents[Option[A]] = new PointerWithEvents[Option[A]](None),
												 equalsCheck: EqualsFunction[A] = EqualsFunction.default)
												(makeDisplay: A => W) =
		new ContainerSingleSelectionManager[A, W, Display, P[A]](container, contentPointer, valuePointer,
			equalsCheck)(makeDisplay)
	
	/**
	  * Creates a content displayer for immutable items that represent a state of some other object
	  * (Eg. different immutable states of a single entity). The states may be linked together via a function
	  * (Eg. by checking related database item row id)
	  * @param container Container that will hold the displays
	  * @param contentPointer Pointer to the displayed content
	  * @param valuePointer A pointer that contains the currently selected value (if any)
	  * @param sameItemCheck Function for checking whether the two items represent the same instance. If you would use
	  *                      a standard equals function (==), please call 'forStatelessItems' instead since
	  *                      equals function is used for checking display equality.
	  * @param makeDisplay Function for creating new displays
	  * @tparam A Type of displayed item
	  * @tparam Display Type of display component
	  * @return New content displayer
	  */
	def forImmutableStates[A, W, Display <: D[A]](container: MutableMultiContainer2[W, Display], contentPointer: P[A],
												  valuePointer: PointerWithEvents[Option[A]] = new PointerWithEvents[Option[A]](None))
												 (sameItemCheck: EqualsFunction[A])
												 (makeDisplay: A => W) =
		new ContainerSingleSelectionManager[A, W, Display, P[A]](container, contentPointer, valuePointer, sameItemCheck,
			Some((a: A, b: A) => a == b))(makeDisplay)
	
	/**
	  * Creates a content displayer for mutable / mutating items. Please note that the items may not always update
	  * correctly since mutations inside the content do not trigger content change events. Therefore you may manually
	  * need to trigger updates for the container's displays.
	  * @param container Container that will hold the displays
	  * @param contentPointer Pointer to the displayed content
	  * @param valuePointer A pointer that contains the currently selected value (if any)
	  * @param sameItemCheck Function for checking whether the two items represent the same instance.
	  *                      (Eg. by checking unique id)
	  * @param equalsCheck Function for checking whether the two items are considered completely equal display-wise
	  * @param makeDisplay Function for creating new displays
	  * @tparam A Type of displayed item
	  * @tparam Display Type of display component
	  * @return New content displayer
	  */
	def forMutableItems[A, W, Display <: D[A]](container: MutableMultiContainer2[W, Display], contentPointer: P[A],
											   valuePointer: PointerWithEvents[Option[A]] = new PointerWithEvents[Option[A]](None))
											  (sameItemCheck: EqualsFunction[A])(equalsCheck: EqualsFunction[A])
											  (makeDisplay: A => W) =
		new ContainerSingleSelectionManager[A, W, Display, P[A]](container, contentPointer, valuePointer, sameItemCheck,
			Some(equalsCheck))(makeDisplay)
}

/**
  * A manager class that handles display container content and manages value selection
  * @author Mikko Hilpinen
  * @since 19.12.2020, v2
  */
class ContainerSingleSelectionManager[A, -W, Display <: Refreshable[A] with ComponentLike2, +PA <: ChangingLike[Vector[A]]]
(container: MutableMultiContainer2[W, Display], contentPointer: PA,
 override val valuePointer: PointerWithEvents[Option[A]] = new PointerWithEvents[Option[A]](None),
 sameItemCheck: EqualsFunction[A] = EqualsFunction.default, equalsCheck: Option[EqualsFunction[A]] = None)
(makeItem: A => W)
	extends ContainerContentDisplayer2[A, W, Display, PA](container, contentPointer, sameItemCheck, equalsCheck)(makeItem)
		with SelectionManager2[A, Option[A], Display, PA]
{
	// ATTRIBUTES	----------------------------
	
	// Updates the display value every time content is updated, because the display may change or be not found anymore
	override lazy val selectedDisplayPointer =
		valuePointer.mergeWith(contentPointer) { (selected, _) => selected.flatMap(displayFor) }
	
	
	// IMPLEMENTED	----------------------------
	
	override protected def itemToSelection(item: A) = Some(item)
	
	override protected def itemInSelection(item: A, selection: Option[A]) = itemToSelection(item)
}
