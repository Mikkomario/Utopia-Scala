package utopia.reach.container.wrapper

import utopia.firmament.component.input.InputWithPointer
import utopia.firmament.drawing.immutable.CustomDrawableFactory
import utopia.firmament.drawing.template.CustomDrawer
import utopia.flow.collection.immutable.caching.cache.Cache
import utopia.flow.collection.immutable.{Empty, Single}
import utopia.flow.util.Mutate
import utopia.flow.view.immutable.eventful.Fixed
import utopia.flow.view.mutable.eventful.SettableFlag
import utopia.flow.view.template.eventful.Changing
import utopia.reach.component.factory.ComponentFactoryFactory.Cff
import utopia.reach.component.factory.FromGenericContextComponentFactoryFactory.Gccff
import utopia.reach.component.factory.contextual.AnyContextContainerBuilderFactory
import utopia.reach.component.factory.{ComponentFactoryFactory, FromGenericContextFactory}
import utopia.reach.component.hierarchy.ComponentHierarchy
import utopia.reach.component.template.{ConcreteCustomDrawReachComponent, PartOfComponentHierarchy, ReachComponent}
import utopia.reach.component.wrapper.{Open, OpenComponent}

trait SwapperSettingsLike[+Repr] extends CustomDrawableFactory[Repr]
{
	// ABSTRACT -------------------------
	
	/**
	  * @return Whether component caching is enabled
	  */
	def isCachingEnabled: Boolean
	/**
	  * @param enabled Whether component caching should be enabled
	  * @return Copy of this item with the specified setting applied
	  */
	def withCachingEnabled(enabled: Boolean): Repr
	
	
	// COMPUTED ------------------------
	
	/**
	  * @return Copy of this item with caching enabled
	  */
	def caching = withCachingEnabled(true)
	/**
	  * @return Copy of this item with caching disabled
	  */
	def notCaching = withCachingEnabled(false)
}

object SwapperSettings
{
	/**
	  * Default swapper settings: No custom drawers, caching enabled
	  */
	lazy val default = apply()
}
case class SwapperSettings(customDrawers: Seq[CustomDrawer] = Empty, isCachingEnabled: Boolean = true)
	extends SwapperSettingsLike[SwapperSettings]
{
	override def withCachingEnabled(enabled: Boolean): SwapperSettings =
		if (isCachingEnabled == enabled) this else copy(isCachingEnabled = enabled)
	override def withCustomDrawers(drawers: Seq[CustomDrawer]): SwapperSettings = copy(customDrawers = drawers)
}

trait SwapperSettingsWrapper[+Repr] extends SwapperSettingsLike[Repr]
{
	// ABSTRACT -------------------------
	
	/**
	  * @return Utilized settings
	  */
	def settings: SwapperSettings
	/**
	  * @param settings New settings to utilize
	  * @return Copy of this item with the specified settings
	  */
	def withSettings(settings: SwapperSettings): Repr
	
	
	// IMPLEMENTED  ---------------------
	
	override def isCachingEnabled: Boolean = settings.isCachingEnabled
	override def withCachingEnabled(enabled: Boolean): Repr = mapSettings { _.withCachingEnabled(enabled) }
	
	override def customDrawers: Seq[CustomDrawer] = settings.customDrawers
	override def withCustomDrawers(drawers: Seq[CustomDrawer]): Repr = mapSettings { _.withCustomDrawers(drawers) }
	
	
	// OTHER    -------------------------
	
	def mapSettings(f: Mutate[SwapperSettings]) = withSettings(f(settings))
}

trait SwapperFactoryLike[+Repr] extends PartOfComponentHierarchy with SwapperSettingsWrapper[Repr]
{
	protected def _apply[A](valuePointer: Changing[A])(makeContent: A => OpenComponent[ReachComponent, _]) =
		new Swapper[A](hierarchy, valuePointer, customDrawers, isCachingEnabled)(makeContent)
}

case class SwapperFactory(hierarchy: ComponentHierarchy, settings: SwapperSettings)
	extends SwapperFactoryLike[SwapperFactory] with FromGenericContextFactory[Any, ContextualSwapperFactory]
{
	// IMPLEMENTED	-------------------------
	
	override def withContext[N <: Any](context: N) =
		ContextualSwapperFactory(hierarchy, context, settings)
	override def withSettings(settings: SwapperSettings): SwapperFactory = copy(settings = settings)
	
	
	// OTHER	-----------------------------
	
	/**
	  * Creates a new swapper
	  * @param valuePointer Pointer to the mirrored value
	  * @param makeContent A function for producing a new component for an item
	  * @tparam A Type of items being mirrored
	  * @return A new swapper container
	  */
	def apply[A](valuePointer: Changing[A])(makeContent: A => OpenComponent[ReachComponent, _]) =
		_apply(valuePointer)(makeContent)
	
	/**
	  * @param contentFactory A factory for building displayed views
	  * @tparam F Type of actual factories used for building content
	  * @return A new view swapper builder
	  */
	def build[F](contentFactory: ComponentFactoryFactory[F]) =
		SwapperBuilder[F](hierarchy, settings, contentFactory)
}

case class SwapperBuilder[+F](hierarchy: ComponentHierarchy, settings: SwapperSettings,
                              contentFactory: ComponentFactoryFactory[F])
	extends SwapperFactoryLike[SwapperBuilder[F]]
{
	// IMPLEMENTED  ---------------------
	
	override def withSettings(settings: SwapperSettings): SwapperBuilder[F] = copy(settings = settings)
	
	
	// OTHER    -------------------------
	
	/**
	  * Creates a new swapper
	  * @param valuePointer A pointer to the mirrored value
	  * @param makeContent A function for producing a component using component a creation factory and a displayed item
	  * @tparam A Type of mirrored value
	  * @return A new swapper
	  */
	def apply[A](valuePointer: Changing[A])(makeContent: (F, A) => ReachComponent) =
		_apply(valuePointer) { item => Open.using(contentFactory) { makeContent(_, item) } }
}

case class ContextualSwapperFactory[N](hierarchy: ComponentHierarchy, context: N, settings: SwapperSettings)
	extends AnyContextContainerBuilderFactory[N, SwapperFactory, ContextualSwapperBuilder, ContextualSwapperFactory]
		with SwapperFactoryLike[ContextualSwapperFactory[N]]
{
	override def withoutContext = SwapperFactory(hierarchy, settings)
	
	override def withContext[N2 <: Any](newContext: N2) = copy(context = newContext)
	override def withSettings(settings: SwapperSettings): ContextualSwapperFactory[N] = copy(settings = settings)
	
	override def build[F[_]](contentFactory: Gccff[N, F]) =
		ContextualSwapperBuilder[N, F](hierarchy, context, settings, contentFactory)
}

case class ContextualSwapperBuilder[N, +F[_]](hierarchy: ComponentHierarchy, context: N,
                                              settings: SwapperSettings, contentFactory: Gccff[N, F])
	extends SwapperFactoryLike[ContextualSwapperBuilder[N, F]]
{
	// IMPLEMENTED  --------------------------
	
	override def withSettings(settings: SwapperSettings): ContextualSwapperBuilder[N, F] = copy(settings = settings)
	
	
	// OTHER    ------------------------------
	
	/**
	  * Creates a new swapper
	  * @param valuePointer A pointer to the mirrored value
	  * @param makeContent A function for producing a component using component a creation factory and a displayed item
	  * @tparam A Type of mirrored value
	  * @return A new swapper
	  */
	def apply[A](valuePointer: Changing[A])(makeContent: (F[N], A) => ReachComponent) =
		_apply[A](valuePointer) { item => Open.withContext(context)(contentFactory) { makeContent(_, item) } }
}

object Swapper extends Cff[SwapperFactory] with Gccff[Any, ContextualSwapperFactory]
{
	override def apply(hierarchy: ComponentHierarchy): SwapperFactory =
		SwapperFactory(hierarchy, SwapperSettings.default)
	
	override def withContext[N <: Any](hierarchy: ComponentHierarchy, context: N): ContextualSwapperFactory[N] =
		ContextualSwapperFactory(hierarchy, context, SwapperSettings.default)
}
/**
  * A component container which swaps the displayed component based on a pointer value.
  * @author Mikko Hilpinen
  * @since 10.1.2025, v1.5
  * @tparam A Type of mirrored value
  * @param hierarchy Component hierarchy this container is attached to
  * @param valuePointer Pointer to the currently selected value
  * @param customDrawers Custom drawers used in this container (default = empty)
  * @param cachingEnabled Whether the created components should be cached & reused / kept in memory (default = true)
  */
class Swapper[A](override val hierarchy: ComponentHierarchy, override val valuePointer: Changing[A],
                 override val customDrawers: Seq[CustomDrawer] = Empty, cachingEnabled: Boolean = true)
                (makeContent: A => OpenComponent[ReachComponent, _])
	extends ConcreteCustomDrawReachComponent with InputWithPointer[A, Changing[A]]
{
	// ATTRIBUTES	-------------------------------
	
	private val contentPointer = valuePointer.fixedValue match {
		// Case: Content won't change => Creates and attaches the component to wrap
		case Some(onlyValue) => Fixed(makeContent(onlyValue).attachTo(this).child)
		// Case: Content changes => Applies swapping
		case None =>
			// Case: Caching is enabled => Keeps components cached and re-attachable
			if (cachingEnabled) {
				def createComponent(value: A) = {
					// Creates the item in open form first
					val open = makeContent(value)
					// Attaches this item based on the selected value
					open.attachTo(this, valuePointer.map { _ == value }).child
				}
				
				val cache = {
					// Case: Reflected pointer may stop changing
					//       => Once/if it does, clears all other components from the memory
					if (valuePointer.destiny.isPossibleToSeal) {
						val cache = Cache.clearable(createComponent)
						valuePointer.onceChangingStops { cache.clear() }
						cache
					}
					// Case: Reflected pointer is not expected to stop changing
					//       => Keeps all created components in memory
					else
						Cache(createComponent)
				}
				valuePointer.map(cache.apply)
			}
			// Case: Caching is disabled => Permanently disconnects components
			else
				valuePointer.map { value: A =>
					// Creates the component in an open form
					val open = makeContent(value)
					
					// The component gets disconnected permanently at the next switch event
					val disconnectFlag = SettableFlag()
					valuePointer.onNextChange { _ => disconnectFlag.set() }
					
					open.attachTo(this, !disconnectFlag).child
				}
	}
	
	
	// INITIAL CODE	-------------------------------
	
	// Revalidates this container whenever content is swapped
	contentPointer.addListenerWhile(hierarchy.linkedFlag) { _ => revalidate() }
	
	
	// COMPUTED	-----------------------------------
	
	/**
	  * @return Currently displayed component
	  */
	private def content = contentPointer.value
	
	
	// IMPLEMENTED	-------------------------------
	
	override def children: Seq[ReachComponent] = Single(content)
	
	// Sets the content size equal to this container's size
	override def updateLayout() = content.size = size
	
	// Uses content's stack size directly
	override def calculatedStackSize = content.stackSize
}
