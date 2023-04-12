package utopia.reflection.controller.data

import utopia.firmament.component.container.many.MutableMultiContainer
import utopia.flow.operator.EqualsFunction
import utopia.flow.view.template.eventful.Changing
import utopia.reflection.component.template.ReflectionComponentLike
import utopia.firmament.component.display.Refreshable
import utopia.firmament.controller.data.ContentDisplayer
import utopia.reflection.component.template.layout.stack.ReflectionStackable

@deprecated("Replaced with a new version in Firmament", "v2.0")
object ContainerContentDisplayer
{
	/**
	 * Container that holds multiple items and is stackable
	 */
	private type MultiStack[X <: ReflectionComponentLike] = MutableMultiContainer[X, X] with ReflectionStackable
	
	/**
	 * Short version of typical pointer used in these methods
	 */
	private type P[X] = Changing[Vector[X]]
	
	/**
	 * Creates a content displayer for immutable items that don't represent state of any other object. No two different
	 * items will be linked in any way.
	 * @param container Container that will hold the displays
	 * @param contentPointer Pointer to the displayed content
	 * @param equalsCheck Function for checking item equality (default = standard equals (== -operator))
	 * @param makeDisplay Function for creating new displays
	 * @tparam A Type of displayed item
	 * @tparam Display Type of display component
	 * @return New content displayer
	 */
	def forStatelessItems[A, Display <: ReflectionStackable with Refreshable[A]]
		(container: MultiStack[Display], contentPointer: P[A],
		 equalsCheck: EqualsFunction[A] = EqualsFunction.default)(makeDisplay: A => Display) =
		new ContainerContentDisplayer[A, MultiStack[Display], Display, P[A]](
			container, contentPointer, equalsCheck)(makeDisplay)
	
	/**
	 * Creates a content displayer for immutable items that represent a state of some other object
	 * (Eg. different immutable states of a single entity). The states may be linked together via a function
	 * (Eg. by checking related database item row id)
	 * @param container Container that will hold the displays
	 * @param contentPointer Pointer to the displayed content
	 * @param sameItemCheck Function for checking whether the two items represent the same instance. If you would use
	 *                      a standard equals function (==), please call 'forStatelessItems' instead since
	 *                      equals function is used for checking display equality.
	 * @param makeDisplay Function for creating new displays
	 * @tparam A Type of displayed item
	 * @tparam Display Type of display component
	 * @return New content displayer
	 */
	def forImmutableStates[A, Display <: ReflectionStackable with Refreshable[A]]
		(container: MultiStack[Display], contentPointer: P[A])(
			sameItemCheck: EqualsFunction[A])(makeDisplay: A => Display) =
		new ContainerContentDisplayer[A, MultiStack[Display], Display, P[A]](container, contentPointer, sameItemCheck,
			Some((a: A, b: A) => a == b))(makeDisplay)
	
	/**
	 * Creates a content displayer for mutable / mutating items. Please note that the items may not always update
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
	 * @return New content displayer
	 */
	def forMutableItems[A, Display <: ReflectionStackable with Refreshable[A]]
		(container: MultiStack[Display], contentPointer: P[A])(
			sameItemCheck: EqualsFunction[A])(equalsCheck: EqualsFunction[A])(makeDisplay: A => Display) =
		new ContainerContentDisplayer[A, MultiStack[Display], Display, P[A]](container, contentPointer, sameItemCheck,
			Some(equalsCheck))(makeDisplay)
}

/**
  * This content manager reflects content changes on a container
  * @author Mikko Hilpinen
  * @since 9.5.2020, v1.2
  * @tparam A The type of content displayed in the container
 *  @tparam Container The type of container managed through this class
  * @tparam Display The type of display where a single item is displayed
 *  @tparam P Type of pointer reflected by this displayer
  * @param container The container managed through this manager
  * @param contentPointer Source of managed content
  * @param sameItemCheck A check for whether the two items represent same different versions of a same instance
  *                      (for immutable instances, a simple equality check). Defaults to an equality check (== -operator)
  * @param equalsCheck A function for checking whether two items should be considered exactly equal. You only need to
  *                    specify this function if the 'sameItemCheck' doesn't test for exact equality. Defaults to None
  *                    (= 'sameItemCheck' is enough)
  * @param makeItem A function for producing new displays
  */
@deprecated("Replaced with a new version in Firmament", "v2.0")
class ContainerContentDisplayer[A, Container <: MutableMultiContainer[Display, Display] with ReflectionStackable,
	Display <: ReflectionStackable with Refreshable[A], +P <: Changing[Vector[A]]]
(protected val container: Container, override val contentPointer: P,
 sameItemCheck: EqualsFunction[A] = EqualsFunction.default, equalsCheck: Option[EqualsFunction[A]] = None)
(makeItem: A => Display) extends ContentDisplayer[A, Display, P]
{
	// ATTRIBUTES   -----------------------
	
	private var displayRemovalListeners = Vector[Display => Unit]()
	
	
	// INITIAL CODE	-----------------------
	
	setup()
	
	
	// IMPLEMENTED	-----------------------
	
	override protected def representSameItem(a: A, b: A) = sameItemCheck(a, b)
	
	override protected def contentIsStateless = equalsCheck.isEmpty
	
	override protected def itemsAreEqual(a: A, b: A) = equalsCheck.map { _(a, b) }.getOrElse(sameItemCheck(a, b))
	
	override def displays = Vector.from(container.components)
	
	override protected def addDisplaysFor(values: Vector[A], index: Int) = container.insertMany(values.map(makeItem), index)
	
	override protected def dropDisplaysAt(range: Range) = container.removeComponentsIn(range).foreach { c =>
		displayRemovalListeners.foreach { _(c) } }
	
	override protected def finalizeRefresh() = container.revalidate()
	
	
	// OTHER    ---------------------------
	
	/**
	 * Registers a function that will be called for each display that is removed from the managed container
	 * @param listener Listener function that takes a display
	 */
	def addDisplayRemovalListener(listener: Display => Unit) = displayRemovalListeners :+= listener
}