package utopia.reflection.controller.data

import utopia.flow.datastructure.mutable.PointerWithEvents
import utopia.reflection.component.Refreshable
import utopia.reflection.component.stack.Stackable
import utopia.reflection.container.stack.MultiStackContainer

object ContainerContentManager
{
	/**
	  * Creates a content manager for immutable items that don't represent state of any other object. No two different
	  * items will be linked in any way.
	  * @param container Container that will hold the displays
	  * @param contentPointer Pointer to the displayed content
	  * @param equalsCheck Function for checking item equality (default = standard equals (== -operator))
	  * @param makeDisplay Function for creating new displays
	  * @tparam A Type of displayed item
	  * @tparam Display Type of display component
	  * @return New content manager
	  */
	def forStatelessItemsPointer[A, Display <: Stackable with Refreshable[A]]
	(container: MultiStackContainer[Display], contentPointer: PointerWithEvents[Vector[A]],
	 equalsCheck: (A, A) => Boolean = { (a: A, b: A) => a == b })(makeDisplay: A => Display) =
		new ContainerContentManager[A, MultiStackContainer[Display], Display](container, contentPointer, equalsCheck)(makeDisplay)
	
	/**
	  * Creates a content manager for immutable items that don't represent state of any other object. No two different
	  * items will be linked in any way.
	  * @param container Container that will hold the displays
	  * @param initialItems Initially displayed content (default = empty vector)
	  * @param equalsCheck Function for checking item equality (default = standard equals (== -operator))
	  * @param makeDisplay Function for creating new displays
	  * @tparam A Type of displayed item
	  * @tparam Display Type of display component
	  * @return New content manager
	  */
	def forStatelessItems[A, Display <: Stackable with Refreshable[A]]
	(container: MultiStackContainer[Display], initialItems: Vector[A] = Vector(),
	 equalsCheck: (A, A) => Boolean = { (a: A, b: A) => a == b })(makeDisplay: A => Display) =
		forStatelessItemsPointer[A, Display](container, new PointerWithEvents(initialItems), equalsCheck)(makeDisplay)
	
	/**
	  * Creates a content manager for immutable items that represent a state of some other object
	  * (Eg. different immutable states of a single entity). The states may be linked together via a function
	  * (Eg. by checking related database item row id)
	  * @param container Container that will hold the displays
	  * @param contentPointer Pointer to the displayed content
	  * @param sameItemCheck Function for checking whether the two items represent the same instance. If you would use
	  *                      a standard equals function (==), please call 'forStatelessItemsPointer' instead since
	  *                      equals function is used for checking display equality.
	  * @param makeDisplay Function for creating new displays
	  * @tparam A Type of displayed item
	  * @tparam Display Type of display component
	  * @return New content manager
	  */
	def forImmutableStatesPointer[A, Display <: Stackable with Refreshable[A]]
	(container: MultiStackContainer[Display], contentPointer: PointerWithEvents[Vector[A]])(
		sameItemCheck: (A, A) => Boolean)(makeDisplay: A => Display) =
		new ContainerContentManager[A, MultiStackContainer[Display], Display](container, contentPointer, sameItemCheck,
			Some((a: A, b: A) => a == b))(makeDisplay)
	
	/**
	  * Creates a content manager for immutable items that represent a state of some other object
	  * (Eg. different immutable states of a single entity). The states may be linked together via a function
	  * (Eg. by checking related database item row id)
	  * @param container Container that will hold the displays
	  * @param initialItems Initially displayed content (default = empty vector)
	  * @param sameItemCheck Function for checking whether the two items represent the same instance. If you would use
	  *                      a standard equals function (==), please call 'forStatelessItems' instead since
	  *                      equals function is used for checking display equality.
	  * @param makeDisplay Function for creating new displays
	  * @tparam A Type of displayed item
	  * @tparam Display Type of display component
	  * @return New content manager
	  */
	def forImmutableStates[A, Display <: Stackable with Refreshable[A]]
	(container: MultiStackContainer[Display], initialItems: Vector[A] = Vector())(
		sameItemCheck: (A, A) => Boolean)(makeDisplay: A => Display) =
		forImmutableStatesPointer[A, Display](container, new PointerWithEvents(initialItems))(sameItemCheck)(makeDisplay)
	
	/**
	  * Creates a content manager for mutable / mutating items. Please note that the items may not always update
	  * correctly since mutations inside the content do not trigger content change events. Therefore you may manually
	  * need to trigger updates for the container's displays.
	  * @param container Container that will hold the displays
	  * @param contentPointer Pointer to the displayed content
	  * @param sameItemCheck Function for checking whether the two items represent the same instance.
	  *                      (Eg. by checking unique id)
	  * @param equalsCheck Function for checking whether the two items are considered completely equal display-wise
	  * @param makeDisplay Function for creating new displays
	  * @tparam A Type of displayed item
	  * @tparam Display Type of display component
	  * @return New content manager
	  */
	def forMutableItemsPointer[A, Display <: Stackable with Refreshable[A]]
	(container: MultiStackContainer[Display], contentPointer: PointerWithEvents[Vector[A]])(
		sameItemCheck: (A, A) => Boolean)(equalsCheck: (A, A) => Boolean)(makeDisplay: A => Display) =
		new ContainerContentManager[A, MultiStackContainer[Display], Display](container, contentPointer, sameItemCheck,
			Some(equalsCheck))(makeDisplay)
	
	/**
	  * Creates a content manager for mutable / mutating items. Please note that the items may not always update
	  * correctly since mutations inside the content do not trigger content change events. Therefore you may manually
	  * need to trigger updates for the container's displays.
	  * @param container Container that will hold the displays
	  * @param initialItems Initially displayed content (default = empty vector)
	  * @param sameItemCheck Function for checking whether the two items represent the same instance.
	  *                      (Eg. by checking unique id)
	  * @param equalsCheck Function for checking whether the two items are considered completely equal display-wise
	  * @param makeDisplay Function for creating new displays
	  * @tparam A Type of displayed item
	  * @tparam Display Type of display component
	  * @return New content manager
	  */
	def forMutableItems[A, Display <: Stackable with Refreshable[A]]
	(container: MultiStackContainer[Display], initialItems: Vector[A] = Vector())(
		sameItemCheck: (A, A) => Boolean)(equalsCheck: (A, A) => Boolean)(makeDisplay: A => Display) =
		forMutableItemsPointer[A, Display](container, new PointerWithEvents(initialItems))(sameItemCheck)(equalsCheck)(makeDisplay)
}

/**
  * This content manager handles content changes for a StackableMultiContainer
  * @author Mikko Hilpinen
  * @since 5.6.2019, v1
  * @tparam A The type of content displayed in the container
 *  @tparam Container The type of container managed through this class
  * @tparam Display The type of display where a single item is displayed
  * @param container The container managed through this manager
  * @param contentPointer Pointer which holds the managed content (default = new pointer)
  * @param sameItemCheck A check for whether the two items represent same different versions of a same instance
  *                      (for immutable instances, a simple equality check). Defaults to an equality check (== -operator)
  * @param equalsCheck A function for checking whether two items should be considered exactly equal. You only need to
  *                    specify this function if the 'sameItemCheck' doesn't test for exact equality. Defaults to None
  *                    (= 'sameItemCheck' is enough)
  * @param makeItem A function for producing new displays
  */
class ContainerContentManager[A, Container <: MultiStackContainer[Display], Display <: Stackable with Refreshable[A]]
(protected val container: Container,
 override val contentPointer: PointerWithEvents[Vector[A]] = new PointerWithEvents[Vector[A]](Vector()),
 sameItemCheck: (A, A) => Boolean = { (a: A, b: A) =>  a == b }, equalsCheck: Option[(A, A) => Boolean] = None)
(makeItem: A => Display) extends ContentManager[A, Display]
{
	// INITIAL CODE	-----------------------
	
	setup()
	
	
	// IMPLEMENTED	-----------------------
	
	override protected def representSameItem(a: A, b: A) = sameItemCheck(a, b)
	
	override protected def contentIsStateless = equalsCheck.isEmpty
	
	override protected def itemsAreEqual(a: A, b: A) = equalsCheck.map { _(a, b) }.getOrElse(sameItemCheck(a, b))
	
	override def displays = container.components
	
	override protected def addDisplaysFor(values: Vector[A], index: Int) = container.insertMany(values.map(makeItem), index)
	
	override protected def dropDisplaysAt(range: Range) = container.removeComponentsIn(range)
	
	override protected def finalizeRefresh() = container.revalidate()
}