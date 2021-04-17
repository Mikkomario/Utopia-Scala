package utopia.reach.container

import utopia.flow.caching.multi.{Cache, CacheLike}
import utopia.flow.datastructure.mutable.PointerWithEvents
import utopia.flow.event.ChangingLike
import utopia.reach.component.factory.ContextInsertableComponentFactoryFactory.ContextualBuilderContentFactory
import utopia.reach.component.factory.{AnyContextContainerBuilderFactory, ComponentFactoryFactory, ContextInsertableComponentFactory, ContextInsertableComponentFactoryFactory, ContextualComponentFactory}
import utopia.reach.component.hierarchy.ComponentHierarchy
import utopia.reach.component.template.{CustomDrawReachComponent, ReachComponentLike}
import utopia.reach.component.wrapper.{Open, OpenComponent}
import utopia.reflection.component.drawing.template.CustomDrawer
import utopia.reflection.component.template.input.{InputWithPointer, InteractionWithPointer}

object CachingViewSwapper extends ContextInsertableComponentFactoryFactory[Any, CachingViewSwapperFactory,
	ContextualCachingViewSwapperFactory]
{
	// IMPLEMENTED	-------------------------
	
	override def apply(hierarchy: ComponentHierarchy) = new CachingViewSwapperFactory(hierarchy)
	
	
	// EXTENSIONS	------------------------
	
	// Swappers using a mutable pointer can be considered mutable themselves
	implicit class MutableCachingViewSwapper[A, +C <: ReachComponentLike](val s: CachingViewSwapper[A, C, PointerWithEvents[A]])
		extends InteractionWithPointer[A]
	{
		override def valuePointer = s.valuePointer
	}
}

class CachingViewSwapperFactory(parentHierarchy: ComponentHierarchy)
	extends ContextInsertableComponentFactory[Any, ContextualCachingViewSwapperFactory]
{
	// IMPLICIT	-----------------------------
	
	implicit def canvas: ReachCanvas = parentHierarchy.top
	
	
	// IMPLEMENTED	-------------------------
	
	override def withContext[N <: Any](context: N) =
		ContextualCachingViewSwapperFactory(this, context)
	
	
	// OTHER	-----------------------------
	
	/**
	  * Creates a new swapper
	  * @param valuePointer Pointer to the mirrored value
	  * @param customDrawers Custom drawers assigned to this container (default = empty)
	  * @param makeContent A function for producing a new component for an item
	  *                    (called only once for each encountered item)
	  * @tparam A Type of items being mirrored
	  * @tparam C Type of display / wrapped components
	  * @tparam P Value pointer type
	  * @return A new swapper container
	  */
	def apply[A, C <: ReachComponentLike, P <: ChangingLike[A]](valuePointer: P,
																customDrawers: Vector[CustomDrawer] = Vector())
															   (makeContent: A => OpenComponent[C, _]) =
		new CachingViewSwapper[A, C, P](parentHierarchy, valuePointer, customDrawers)(makeContent)
	
	/**
	  * Creates a new swapper, uses the most generic available type arguments (Use this method when you don't want to
	  * specify type arguments)
	  * @param valuePointer Pointer to the mirrored value
	  * @param customDrawers Custom drawers assigned to this container (default = empty)
	  * @param makeContent A function for producing a new component for an item
	  *                    (called only once for each encountered item)
	  * @tparam A Type of items being mirrored
	  * @return A new swapper container
	  */
	def generic[A](valuePointer: ChangingLike[A], customDrawers: Vector[CustomDrawer] = Vector())
				  (makeContent: A => OpenComponent[ReachComponentLike, _]) =
		apply[A, ReachComponentLike, ChangingLike[A]](valuePointer, customDrawers)(makeContent)
}

case class ContextualCachingViewSwapperFactory[N](factory: CachingViewSwapperFactory, context: N)
	extends AnyContextContainerBuilderFactory[N, CachingViewSwapperFactory, ContextualViewSwapperBuilder,
		ContextualCachingViewSwapperFactory]
{
	override def withoutContext = factory
	
	override def withContext[N2 <: Any](newContext: N2) =
		copy(context = newContext)
	
	override def build[F[X] <: ContextualComponentFactory[X, _ >: N, F]](contentFactory: ContextualBuilderContentFactory[N, F]) =
		new ContextualViewSwapperBuilder[N, F](factory, context, contentFactory)
}

class CachingViewSwapperBuilder[+F](factory: CachingViewSwapperFactory, contentFactory: ComponentFactoryFactory[F])
{
	import factory.canvas
	
	/**
	  * Creates a new swapper
	  * @param valuePointer A pointer to the mirrored value
	  * @param customDrawers Custom drawers assigned to this swapper (default = empty)
	  * @param makeContent A function for producing a component using component a creation factory and a displayed item
	  * @tparam A Type of mirrored value
	  * @tparam C Type of display / wrapped component
	  * @tparam P Type of value pointer used
	  * @return A new swapper
	  */
	def apply[A, C <: ReachComponentLike, P <: ChangingLike[A]](valuePointer: P,
																customDrawers: Vector[CustomDrawer] = Vector())
															   (makeContent: (F, A) => C) =
		factory[A, C, P](valuePointer, customDrawers) { item => Open.using(contentFactory) { makeContent(_, item) } }
	
	/**
	  * Creates a new swapper without specifying additional type arguments
	  * @param valuePointer A pointer to the mirrored value
	  * @param customDrawers Custom drawers assigned to this swapper (default = empty)
	  * @param makeContent A function for producing a component using component a creation factory and a displayed item
	  * @tparam A Type of mirrored value
	  * @return A new swapper
	  */
	def generic[A](valuePointer: ChangingLike[A], customDrawers: Vector[CustomDrawer] = Vector())
				  (makeContent: (F, A) => ReachComponentLike) =
		apply[A, ReachComponentLike, ChangingLike[A]](valuePointer, customDrawers)(makeContent)
}

class ContextualViewSwapperBuilder[N, +F[X] <: ContextualComponentFactory[X, _ >: N, F]]
(factory: CachingViewSwapperFactory, context: N, contentFactory: ContextualBuilderContentFactory[N, F])
{
	import factory.canvas
	
	/**
	  * Creates a new swapper
	  * @param valuePointer A pointer to the mirrored value
	  * @param customDrawers Custom drawers assigned to this swapper (default = empty)
	  * @param makeContent A function for producing a component using component a creation factory and a displayed item
	  * @tparam A Type of mirrored value
	  * @tparam C Type of display / wrapped component
	  * @tparam P Type of value pointer used
	  * @return A new swapper
	  */
	def apply[A, C <: ReachComponentLike, P <: ChangingLike[A]](valuePointer: P,
																customDrawers: Vector[CustomDrawer] = Vector())
															   (makeContent: (F[N], A) => C) =
		factory[A, C, P](valuePointer, customDrawers) { item =>
			Open.withContext(contentFactory, context) { makeContent(_, item) }
		}
	
	/**
	  * Creates a new swapper
	  * @param valuePointer A pointer to the mirrored value
	  * @param customDrawers Custom drawers assigned to this swapper (default = empty)
	  * @param makeContent A function for producing a component using component a creation factory and a displayed item
	  * @tparam A Type of mirrored value
	  * @return A new swapper
	  */
	def generic[A](valuePointer: ChangingLike[A], customDrawers: Vector[CustomDrawer] = Vector())
				  (makeContent: (F[N], A) => ReachComponentLike) =
		apply[A, ReachComponentLike, ChangingLike[A]](valuePointer, customDrawers)(makeContent)
}

/**
  * A single component container which swaps the displayed component based on a pointer value. This version of this
  * type of component only creates each option once, then caching and reusing it in order to avoid unnecessary
  * component creations
  * @author Mikko Hilpinen
  * @since 16.12.2020, v0.1
  * @param parentHierarchy Component hierarchy this container is attached to
  * @param valuePointer Pointer to the currently selected value
  * @param customDrawers Custom drawers used in this container (default = empty)
  * @tparam A Type of mirrored value
  * @tparam C Type of wrapped component
  * @tparam P Type of value pointer used
  */
class CachingViewSwapper[A, +C <: ReachComponentLike, +P <: ChangingLike[A]]
(override val parentHierarchy: ComponentHierarchy, override val valuePointer: P,
 override val customDrawers: Vector[CustomDrawer] = Vector())(makeContent: A => OpenComponent[C, _])
	extends CustomDrawReachComponent with InputWithPointer[A, ChangingLike[A]]
{
	// ATTRIBUTES	-------------------------------
	
	private val componentCache: CacheLike[A, C] = Cache[A, C] { item =>
		// Creates the item in open form first
		val open = makeContent(item)
		// Attaches this item based on the selected value
		open.attachTo(this, valuePointer.map { _ == item }).child
	}
	
	/**
	  * A pointer to the currently displayed component
	  */
	val contentPointer = valuePointer.map { componentCache(_) }
	
	
	// INITIAL CODE	-------------------------------
	
	// Revalidates this container whenever content is swapped
	contentPointer.addAnyChangeListener { revalidate() }
	
	
	// COMPUTED	-----------------------------------
	
	/**
	  * @return Currently displayed component
	  */
	def content = contentPointer.value
	
	
	// IMPLEMENTED	-------------------------------
	
	override def children = Vector(content)
	
	// Sets the content size equal to this container's size
	override def updateLayout() = content.size = size
	
	// Uses content's stack size directly
	override def calculatedStackSize = content.stackSize
}
