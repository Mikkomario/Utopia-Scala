package utopia.reflection.controller.data

import utopia.flow.datastructure.mutable.PointerWithEvents
import utopia.reflection.component.Refreshable
import utopia.reflection.component.stack.Stackable
import utopia.reflection.container.stack.MultiStackContainer

/**
  * This content manager handles content changes for a StackableMultiContainer
  * @author Mikko Hilpinen
  * @since 5.6.2019, v1
  * @tparam A The type of content displayed in the container
 *  @tparam Container The type of container managed through this class
  * @tparam Display The type of display where a single item is displayed
  * @param container The container managed through this manager
  * @param makeItem A function for producing new displays
  * @param equalsCheck A function for checking whether two items should be considered equal (default = standard equals)
  */
class ContainerContentManager[A, Container <: MultiStackContainer[Display], Display <: Stackable with Refreshable[A]]
(protected val container: Container, equalsCheck: (A, A) => Boolean = { (a: A, b: A) =>  a == b },
 override val contentPointer: PointerWithEvents[Vector[A]] = new PointerWithEvents[Vector[A]](Vector()))
(makeItem: A => Display) extends ContentManager[A, Display]
{
	// INITIAL CODE	-----------------------
	
	setup()
	
	
	// IMPLEMENTED	-----------------------
	
	override protected def itemsAreEqual(a: A, b: A) = equalsCheck(a, b)
	
	override def displays = container.components
	
	override protected def addDisplaysFor(values: Vector[A]) = container ++= values.map(makeItem)
	
	override protected def dropDisplays(dropped: Vector[Display]) = container --= dropped
	
	override protected def finalizeRefresh() = container.revalidate()
}