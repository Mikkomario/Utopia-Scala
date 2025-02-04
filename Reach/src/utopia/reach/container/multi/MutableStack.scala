package utopia.reach.container.multi

import utopia.firmament.component.container.many.MutableMultiContainer
import utopia.firmament.context.base.StaticBaseContext
import utopia.firmament.model.enumeration.StackLayout
import utopia.firmament.model.stack.StackLength
import utopia.flow.collection.immutable.Empty
import utopia.flow.collection.CollectionExtensions._
import utopia.flow.view.mutable.Pointer
import utopia.flow.view.mutable.eventful.EventfulPointer
import utopia.flow.view.template.eventful.Flag
import utopia.paradigm.enumeration.Axis2D
import utopia.reach.component.factory.ComponentFactoryFactory.Cff
import utopia.reach.component.factory.FromContextFactory
import utopia.reach.component.factory.contextual.BaseContextualFactory
import utopia.reach.component.hierarchy.ComponentHierarchy
import utopia.reach.component.template.{MutableConcreteCustomDrawReachComponent, ReachComponent}
import utopia.reach.component.wrapper.OpenComponent

case class MutableStackFactory(hierarchy: ComponentHierarchy, settings: StackSettings = StackSettings.default,
                               margin: StackLength = StackLength.any)
	extends FromContextFactory[StaticBaseContext, ContextualMutableStackFactory]
		with StackSettingsWrapper[MutableStackFactory]
{
	// COMPUTED ----------------------------
	
	/**
	  * @return Copy of this factory without any margin between items
	  */
	def withoutMargin = withMargin(StackLength.fixedZero)
	
	
	// IMPLEMENTED	---------------------------------
	
	override def withSettings(settings: StackSettings): MutableStackFactory = copy(settings = settings)
	
	override def withContext(context: StaticBaseContext) = ContextualMutableStackFactory(hierarchy, context, settings)
	
	
	// OTHER	-------------------------------------
	
	/**
	  * @param margin Margin to place between the items in this stack
	  * @return Copy of this factory with the specified margin
	  */
	def withMargin(margin: StackLength) = copy(margin = margin)
	
	/**
	  * Creates a new stack
	  * @tparam C Type of components within this stack
	  * @return A new stack
	  */
	def apply[C <: ReachComponent]() = {
		val stack = new MutableStack[C](hierarchy, axis, layout, margin, capPointer.value)
		capPointer.addListenerWhile(stack.linkedFlag) { e => stack.cap = e.newValue }
		stack
	}
}

case class ContextualMutableStackFactory(hierarchy: ComponentHierarchy, context: StaticBaseContext,
                                         settings: StackSettings = StackSettings.default, areRelated: Boolean = false)
	extends BaseContextualFactory[ContextualMutableStackFactory]
		with StackSettingsWrapper[ContextualMutableStackFactory]
{
	// COMPUTED ----------------------
	
	/**
	  * @return Copy of this stack factory that places items closer to each other
	  */
	def related = copy(areRelated = true)
	
	/**
	  * @return Copy of this factory that places no margin between the items
	  */
	def withoutMargin = mapContext { _.withoutStackMargin }
	
	
	// IMPLEMENTED	-------------------------------
	
	override def self: ContextualMutableStackFactory = this
	
	override def withSettings(settings: StackSettings): ContextualMutableStackFactory = copy(settings = settings)
	override def withContext(newContext: StaticBaseContext) = copy(context = newContext)
	
	
	// OTHER	-----------------------------------
	
	/**
	  * Creates a new stack
	  * @tparam C Type of components within this stack
	  * @return A new stack
	  */
	def apply[C <: ReachComponent]() = {
		// FIXME: Doesn't support mutating cap at this time
		val stack = new MutableStack[C](hierarchy, axis, layout,
			if (areRelated) context.smallStackMargin else context.stackMargin, capPointer.value)
		stack
	}
}

object MutableStack extends Cff[MutableStackFactory]
{
	override def apply(hierarchy: ComponentHierarchy) = MutableStackFactory(hierarchy)
}

/**
  * A mutable container which "stacks" / places components back to back either horizontally or vertically
  * @author Mikko Hilpinen
  * @since 17.10.2020, v0.1
  */
class MutableStack[C <: ReachComponent](override val hierarchy: ComponentHierarchy,
                                        initialDirection: Axis2D, initialLayout: StackLayout,
                                        initialMargin: StackLength, initialCap: StackLength)
	extends MutableConcreteCustomDrawReachComponent with Stack with MutableMultiContainer[OpenComponent[C, _], C]
{
	// ATTRIBUTES	------------------------
	
	private val _componentsPointer = EventfulPointer[Seq[C]](Empty)
	private var pointers = Map[Int, Pointer[Boolean]]()
	
	private var _direction = initialDirection
	private var _layout = initialLayout
	private var _margin = initialMargin
	private var _cap = initialCap
	
	override lazy val visibilityPointer: Flag = _componentsPointer.map { _.nonEmpty }
	
	
	// COMPUTED -----------------------------
	
	/**
	 * @return A read-only pointer to this stack's current contents
	 */
	def componentsPointer = _componentsPointer.readOnly
	
	
	// IMPLEMENTED	------------------------
	
	override def components = _componentsPointer.value
	
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
		val newComps = components.iterator.filterNot { c => contains(c.component) }.toOptimizedSeq
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
				val newPointer = EventfulPointer(true)
				pointers += (c.component.hashCode() -> newPointer)
				c.hierarchy.complete(this, newPointer)
		}
	}
}
