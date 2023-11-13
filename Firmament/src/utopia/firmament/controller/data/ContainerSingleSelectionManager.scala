package utopia.firmament.controller.data

import utopia.firmament.component.Component
import utopia.firmament.component.container.many.MutableMultiContainer
import utopia.firmament.component.display.Refreshable
import utopia.flow.operator.equality.EqualsFunction
import utopia.flow.view.mutable.eventful.EventfulPointer
import utopia.flow.view.template.eventful.Changing

object ContainerSingleSelectionManager
{
	/**
	  * Short version of typical pointer used in these methods
	  */
	private type P[X] = Changing[Vector[X]]
	
	/**
	  * Short version for a refreshable display component
	  */
	private type D[X] = Component with Refreshable[X]
	
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
	def forStatelessItems[A, W, Display <: D[A]](container: MutableMultiContainer[W, Display],
	                                             contentPointer: P[A],
	                                             valuePointer: EventfulPointer[Option[A]] = new EventfulPointer[Option[A]](None),
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
	def forImmutableStates[A, W, Display <: D[A]](container: MutableMultiContainer[W, Display], contentPointer: P[A],
	                                              valuePointer: EventfulPointer[Option[A]] = new EventfulPointer[Option[A]](None))
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
	def forMutableItems[A, W, Display <: D[A]](container: MutableMultiContainer[W, Display], contentPointer: P[A],
	                                           valuePointer: EventfulPointer[Option[A]] = new EventfulPointer[Option[A]](None))
											  (sameItemCheck: EqualsFunction[A])(equalsCheck: EqualsFunction[A])
											  (makeDisplay: A => W) =
		new ContainerSingleSelectionManager[A, W, Display, P[A]](container, contentPointer, valuePointer, sameItemCheck,
			Some(equalsCheck))(makeDisplay)
}

/**
  * A manager class that handles display container content and manages value selection
  * @author Mikko Hilpinen
  * @since 19.12.2020, Reflection v2
  */
class ContainerSingleSelectionManager[A, -W, Display <: Refreshable[A] with Component, +PA <: Changing[Vector[A]]]
(container: MutableMultiContainer[W, Display], contentPointer: PA,
 override val valuePointer: EventfulPointer[Option[A]] = EventfulPointer.empty(),
 sameItemCheck: EqualsFunction[A] = EqualsFunction.default, equalsCheck: Option[EqualsFunction[A]] = None)
(makeItem: A => W)
	extends ContainerContentDisplayer[A, W, Display, PA](container, contentPointer, sameItemCheck, equalsCheck)(makeItem)
		with SelectionManager[A, Option[A], Display, PA]
{
	// ATTRIBUTES	----------------------------
	
	private val _selectedDisplayPointer = new EventfulPointer[Iterable[Display]](None)
	
	
	// INITIAL CODE ----------------------------
	
	// When selected value changes, updates selected display status as well
	valuePointer.addContinuousListener { e =>
		_selectedDisplayPointer.value = e.newValue.flatMap(displayFor)
	}
	setup()
	
	
	// COMPUTED --------------------------------
	
	override def selectedDisplayPointer: Changing[Iterable[Display]] = _selectedDisplayPointer.view
	
	
	// IMPLEMENTED	----------------------------
	
	override protected def itemToSelection(item: A) = Some(item)
	override protected def itemInSelection(item: A, selection: Option[A]) = itemToSelection(item)
	
	override protected def finalizeRefresh() = {
		super.finalizeRefresh()
		// Updates the selected display at the end of refresh
		// (this is the first place where displays are up-to-date after a content update)
		_selectedDisplayPointer.value = valuePointer.value.flatMap(displayFor)
	}
}
