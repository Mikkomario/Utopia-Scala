package utopia.firmament.controller.data

import utopia.firmament.component.Component
import utopia.firmament.component.container.many.MutableMultiContainer
import utopia.firmament.component.display.Refreshable
import utopia.flow.operator.equality.EqualsFunction
import utopia.flow.view.mutable.async.Volatile
import utopia.flow.view.template.eventful.Changing

object ContainerContentDisplayer
{
	/**
	  * Short version of typical pointer used in these methods
	  */
	private type P[X] = Changing[Seq[X]]
	/**
	  * Short version for a refreshable display component
	  */
	private type D[X] = Component with Refreshable[X]
	
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
	def forStatelessItems[A, W, Display <: D[A]](container: MutableMultiContainer[W, Display],
	                                             contentPointer: P[A],
	                                             equalsCheck: EqualsFunction[A] = EqualsFunction.default)
												(makeDisplay: A => W) =
		apply[A, W, Display, P[A]](container, contentPointer, equalsCheck)(makeDisplay)
	
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
	def forImmutableStates[A, W, Display <: D[A]](container: MutableMultiContainer[W, Display], contentPointer: P[A])
												 (sameItemCheck: EqualsFunction[A])
												 (makeDisplay: A => W) =
		apply[A, W, Display, P[A]](container, contentPointer, sameItemCheck, Some((a: A, b: A) => a == b))(makeDisplay)
	
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
	def forMutableItems[A, W, Display <: D[A]](container: MutableMultiContainer[W, Display], contentPointer: P[A])
											  (sameItemCheck: EqualsFunction[A])(equalsCheck: EqualsFunction[A])
											  (makeDisplay: A => W) =
		apply[A, W, Display, P[A]](container, contentPointer, sameItemCheck, Some(equalsCheck))(makeDisplay)
			
	def apply[A, W, Display <: Refreshable[A] with Component, P <: Changing[Seq[A]]](container: MutableMultiContainer[W, Display],
	                                                                                 contentPointer: P,
	                                                                                 sameItemCheck: EqualsFunction[A] = EqualsFunction.default,
	                                                                                 equalsCheck: Option[EqualsFunction[A]] = None)
	                                                                                (makeItem: A => W) =
	{
		val displayer = new ContainerContentDisplayer[A, W, Display, P](container, contentPointer, sameItemCheck, equalsCheck)(makeItem)
		displayer.setup()
		displayer
	}
}

/**
  * This content manager reflects content changes on a container
  *
  * Note for subclasses: Please call setup() after all attributes have been initialized.
  *
  * @author Mikko Hilpinen
  * @since 9.5.2020, Reflection v1.2
  * @tparam A The type of content displayed in the container
  * @tparam W A display wrapper class used when one is being added to a container
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
class ContainerContentDisplayer[A, -W, Display <: Refreshable[A] with Component,
	+P <: Changing[Seq[A]]] protected(protected val container: MutableMultiContainer[W, Display],
                                         override val contentPointer: P,
                                         sameItemCheck: EqualsFunction[A] = EqualsFunction.default,
                                         equalsCheck: Option[EqualsFunction[A]] = None)
                                        (makeItem: A => W)
	extends ContentDisplayer[A, Display, P]
{
	// ATTRIBUTES   -----------------------
	
	private val capacity = Volatile(Set[Display]())
	
	
	// IMPLEMENTED	-----------------------
	
	override protected def contentIsStateless = equalsCheck.isEmpty
	
	override protected def representSameItem(a: A, b: A) = sameItemCheck(a, b)
	override protected def itemsAreEqual(a: A, b: A) = equalsCheck.getOrElse(sameItemCheck)(a, b)
	
	override def displays = container.components
	
	override protected def addDisplaysFor(values: Seq[A], index: Int) = {
		// Uses stored backup components if possible
		val existingSlots = capacity.mutate { current =>
			if (current.isEmpty)
				current -> current
			else
				current.splitAt(values.size)
		}.toVector
		if (existingSlots.nonEmpty) {
			existingSlots.zip(values).foreach { case (d, v) => d.content = v }
			container.addBack(existingSlots, index)
		}
		// If there weren't enough backup components, creates new ones
		if (values.size > existingSlots.size)
			container.insertMany(values.drop(existingSlots.size).map(makeItem), index + existingSlots.size)
	}
	
	// Stores the removed components to extra capacity (so that they can be reused later)
	override protected def dropDisplaysAt(range: Range) =
		capacity.update { _ ++ container.removeComponentsIn(range) }
	
	override protected def finalizeRefresh() = ()
}