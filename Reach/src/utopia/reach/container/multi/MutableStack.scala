package utopia.reach.container.multi

import utopia.firmament.component.container.many.MutableMultiContainer
import utopia.firmament.context.BaseContext
import utopia.firmament.model.enumeration.StackLayout
import utopia.firmament.model.enumeration.StackLayout.Fit
import utopia.firmament.model.stack.StackLength
import utopia.flow.view.mutable.Pointer
import utopia.flow.view.mutable.eventful.PointerWithEvents
import utopia.flow.view.template.eventful.FlagLike
import utopia.paradigm.enumeration.Axis.{X, Y}
import utopia.paradigm.enumeration.Axis2D
import utopia.reach.component.factory.ComponentFactoryFactory.Cff
import utopia.reach.component.factory.{BaseContextualFactory, FromContextFactory}
import utopia.reach.component.hierarchy.ComponentHierarchy
import utopia.reach.component.template.{MutableCustomDrawReachComponent, ReachComponentLike}
import utopia.reach.component.wrapper.OpenComponent

object MutableStack extends Cff[MutableStackFactory]
{
	override def apply(hierarchy: ComponentHierarchy) = new MutableStackFactory(hierarchy)
}

class MutableStackFactory(hierarchy: ComponentHierarchy)
	extends FromContextFactory[BaseContext, ContextualMutableStackFactory]
{
	// IMPLEMENTED	---------------------------------
	
	override def withContext(context: BaseContext) = ContextualMutableStackFactory(hierarchy, context)
	
	
	// OTHER	-------------------------------------
	
	/**
	  * Creates a new stack
	  * @param direction Direction along which the components are "stacked" (X = row, Y = column (default))
	  * @param layout Layout used when determining component length perpendicular to stack direction
	  *               (default = Fit = All components have same length)
	  * @param margin Margin placed between components (default = any, preferring 0)
	  * @param cap Margin placed at each end of this stack (default = always 0)
	  * @tparam C Type of components within this stack
	  * @return A new stack
	  */
	def apply[C <: ReachComponentLike](direction: Axis2D = Y, layout: StackLayout = Fit,
									   margin: StackLength = StackLength.any, cap: StackLength = StackLength.fixedZero) =
		new MutableStack[C](hierarchy, direction, layout, margin, cap)
	
	/**
	  * Creates a new horizontal stack
	  * @param layout Layout used when determining component length perpendicular to stack direction
	  *               (default = Fit = All components have same length)
	  * @param margin Margin placed between components (default = any, preferring 0)
	  * @param cap Margin placed at each end of this stack (default = always 0)
	  * @tparam C Type of components within this stack
	  * @return A new stack
	  */
	def row[C <: ReachComponentLike](layout: StackLayout = Fit, margin: StackLength = StackLength.any,
									 cap: StackLength = StackLength.fixedZero) =
		apply[C](X, layout, margin, cap)
	
	/**
	  * Creates a new vertical stack
	  * @param layout Layout used when determining component length perpendicular to stack direction
	  *               (default = Fit = All components have same length)
	  * @param margin Margin placed between components (default = any, preferring 0)
	  * @param cap Margin placed at each end of this stack (default = always 0)
	  * @tparam C Type of components within this stack
	  * @return A new stack
	  */
	def column[C <: ReachComponentLike](layout: StackLayout = Fit, margin: StackLength = StackLength.any,
										cap: StackLength = StackLength.fixedZero) =
		apply[C](Y, layout, margin, cap)
}

case class ContextualMutableStackFactory(hierarchy: ComponentHierarchy, context: BaseContext)
	extends BaseContextualFactory[ContextualMutableStackFactory]
{
	// IMPLEMENTED	-------------------------------
	
	override def self: ContextualMutableStackFactory = this
	
	override def withContext(newContext: BaseContext) = copy(context = newContext)
	
	
	// OTHER	-----------------------------------
	
	/**
	  * Creates a new stack
	  * @param direction Direction along which the components are "stacked" (X = row, Y = column (default))
	  * @param layout Layout used when determining component length perpendicular to stack direction
	  *               (default = Fit = All components have same length)
	  * @param cap Margin placed at each end of this stack (default = always 0)
	  * @param areRelated Whether the stacked items should be considered closely related (affects margin)
	  *                   (default = false)
	  * @tparam C Type of components within this stack
	  * @return A new stack
	  */
	def apply[C <: ReachComponentLike](direction: Axis2D = Y, layout: StackLayout = Fit,
									   cap: StackLength = StackLength.fixedZero, areRelated: Boolean = false) =
		new MutableStack[C](hierarchy, direction, layout,
			if (areRelated) context.smallStackMargin else context.stackMargin, cap)
	
	/**
	  * Creates a new stack with no margin between items
	  * @param direction Direction along which the components are "stacked" (X = row, Y = column (default))
	  * @param layout Layout used when determining component length perpendicular to stack direction
	  *               (default = Fit = All components have same length)
	  * @param cap Margin placed at each end of this stack (default = always 0)
	  * @tparam C Type of components within this stack
	  * @return A new stack
	  */
	def withoutMargin[C <: ReachComponentLike](direction: Axis2D = Y, layout: StackLayout = Fit,
											   cap: StackLength = StackLength.fixedZero) =
		new MutableStack[C](hierarchy, direction, layout, StackLength.fixedZero, cap)
	
	/**
	  * Creates a new horizontal stack
	  * @param layout Layout used when determining component length perpendicular to stack direction
	  *               (default = Fit = All components have same length)
	  * @param cap Margin placed at each end of this stack (default = always 0)
	  * @param areRelated Whether the stacked items should be considered closely related (affects margin)
	  *                   (default = false)
	  * @tparam C Type of components within this stack
	  * @return A new stack
	  */
	def row[C <: ReachComponentLike](layout: StackLayout = Fit, cap: StackLength = StackLength.fixedZero,
									 areRelated: Boolean = false) =
		apply[C](X, layout, cap, areRelated)
	
	/**
	  * Creates a new vertical stack
	  * @param layout Layout used when determining component length perpendicular to stack direction
	  *               (default = Fit = All components have same length)
	  * @param cap Margin placed at each end of this stack (default = always 0)
	  * @param areRelated Whether the stacked items should be considered closely related (affects margin)
	  *                   (default = false)
	  * @tparam C Type of components within this stack
	  * @return A new stack
	  */
	def column[C <: ReachComponentLike](layout: StackLayout = Fit, cap: StackLength = StackLength.fixedZero,
									 areRelated: Boolean = false) =
		apply[C](Y, layout, cap, areRelated)
}

/**
  * A mutable container which "stacks" / places components back to back either horizontally or vertically
  * @author Mikko Hilpinen
  * @since 17.10.2020, v0.1
  */
class MutableStack[C <: ReachComponentLike](override val parentHierarchy: ComponentHierarchy,
											initialDirection: Axis2D, initialLayout: StackLayout,
											initialMargin: StackLength, initialCap: StackLength)
	extends MutableCustomDrawReachComponent with Stack[C] with MutableMultiContainer[OpenComponent[C, _], C]
{
	// ATTRIBUTES	------------------------
	
	private val _componentsPointer = new PointerWithEvents[Vector[C]](Vector())
	private var pointers = Map[Int, Pointer[Boolean]]()
	
	private var _direction = initialDirection
	private var _layout = initialLayout
	private var _margin = initialMargin
	private var _cap = initialCap
	
	override lazy val visibilityPointer: FlagLike = _componentsPointer.map { _.nonEmpty }
	
	
	// COMPUTED -----------------------------
	
	/**
	 * @return A read-only pointer to this stack's current contents
	 */
	def componentsPointer = _componentsPointer.view
	
	
	// IMPLEMENTED	------------------------
	
	override def direction = _direction
	def direction_=(newDirection: Axis2D) = {
		if (_direction != newDirection) {
			_direction = newDirection
			revalidate()
		}
	}
	override def layout = _layout
	def layout_=(newLayout: StackLayout) = {
		if (_layout != newLayout) {
			_layout = newLayout
			revalidate()
		}
	}
	override def margin = _margin
	def margin_=(newMargin: StackLength) = {
		if (_margin != newMargin) {
			_margin = newMargin
			revalidate()
		}
	}
	override def cap = _cap
	def cap_=(newCap: StackLength) = {
		if (_cap != newCap) {
			_cap = newCap
			revalidate()
		}
	}
	
	override protected def add(component: OpenComponent[C, _], index: Int) = {
		if (!contains(component.component)) {
			_componentsPointer.update { old => (old.take(index) :+ component.component) ++ old.drop(index) }
			updatePointerFor(component)
			revalidate()
		}
	}
	override protected def add(components: IterableOnce[OpenComponent[C, _]], index: Int) = {
		// Needs to buffer the components (iterating multiple times)
		val newComps = components.iterator.filterNot { c => contains(c.component) }.toVector
		if (newComps.nonEmpty) {
			_componentsPointer.update { old => old.take(index) ++ newComps.map { _.component } ++ old.drop(index) }
			newComps.foreach(updatePointerFor)
			revalidate()
		}
	}
	
	// Disconnects the component and revalidates this container
	override protected def remove(component: C) = {
		_componentsPointer.update { _.filterNot { _ == component } }
		pointers.get(component.hashCode()).foreach { _.value = false }
		revalidate()
	}
	override protected def remove(components: IterableOnce[C]) = {
		val buffered = Set.from(components)
		_componentsPointer.update { _.filterNot(buffered.contains) }
		buffered.iterator.flatMap { c => pointers.get(c.hashCode()) }.foreach { _.value = false }
		revalidate()
	}
	
	override def components = _componentsPointer.value
	
	/**
	  * Adds a previously added component back to this stack
	  * @param component Component to add
	  * @param index Index where to add the component
	  * @throws NoSuchElementException If the specified component has never been added to this stack previously
	  */
	@throws[NoSuchElementException]
	override def addBack(component: C, index: Int = components.size) = {
		if (!contains(component)) {
			_componentsPointer.update { old => (old.take(index) :+ component) ++ old.drop(index) }
			pointers(component.hashCode()).value = true
			revalidate()
		}
	}
	override def addBack(components: IterableOnce[C], index: Int) = {
		val newComps = components.iterator.filterNot(contains).toVector
		if (newComps.nonEmpty) {
			_componentsPointer.update { old =>  old.take(index) ++ newComps ++ old.drop(index) }
			newComps.foreach { c => pointers(c.hashCode()).value = true }
			revalidate()
		}
	}
	
	
	// OTHER	-----------------------------
	
	/**
	  * @param component A tested component
	  * @return Whether this stack contains the specified component
	  */
	def contains(component: C) = components.contains(component)
	
	private def updatePointerFor(c: OpenComponent[C, _]) = {
		pointers.get(c.component.hashCode()) match {
			case Some(existingPointer) => existingPointer.value = true
			case None =>
				val newPointer = new PointerWithEvents(true)
				pointers += (c.component.hashCode() -> newPointer)
				c.hierarchy.complete(this, newPointer)
		}
	}
}
