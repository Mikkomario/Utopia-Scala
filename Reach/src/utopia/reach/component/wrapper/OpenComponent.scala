package utopia.reach.component.wrapper

import utopia.firmament.context.BaseContext
import utopia.firmament.drawing.immutable.BackgroundDrawer
import utopia.firmament.drawing.template.CustomDrawer
import utopia.firmament.model.enumeration.StackLayout
import utopia.firmament.model.enumeration.StackLayout.Fit
import utopia.firmament.model.stack.{StackInsetsConvertible, StackLength}
import utopia.flow.collection.immutable.{Empty, Single}
import utopia.flow.view.immutable.eventful.AlwaysTrue
import utopia.flow.view.template.eventful.Changing
import utopia.paradigm.color.Color
import utopia.paradigm.enumeration.Axis.{X, Y}
import utopia.paradigm.enumeration.Axis2D
import utopia.reach.component.factory.FromContextComponentFactoryFactory.Ccff
import utopia.reach.component.factory.FromGenericContextComponentFactoryFactory.Gccff
import utopia.reach.component.factory.{ComponentFactoryFactory, FromContextComponentFactoryFactory, FromGenericContextFactory}
import utopia.reach.component.hierarchy.{ComponentHierarchy, SeedHierarchyBlock}
import utopia.reach.component.template.ReachComponentLike
import utopia.reach.component.wrapper.ComponentCreationResult.CreationsResult
import utopia.reach.container.ReachCanvas
import utopia.reach.container.layered.LayerPositioning
import utopia.reach.container.multi.{Stack, StackSettings}
import utopia.reach.container.wrapper.Framing

import scala.language.implicitConversions

object Open extends FromGenericContextFactory[Any, ContextualOpenComponentFactory]
{
	// IMPLEMENTED  -----------------------
	
	override def withContext[N <: Any](context: N): ContextualOpenComponentFactory[N] =
		ContextualOpenComponentFactory(context)
	
	
	// OTHER    ---------------------------
	
	/**
	  * Creates a new open component
	  * @param creation Component creation function (returns a component and a possible additional result)
	  * @param canvas Implicit access to top canvas component
	  * @tparam C Type of created component
	  * @tparam R Type of additional creation result
	  * @return A new open component
	  */
	def apply[C, R](creation: ComponentHierarchy => ComponentCreationResult[C, R])
	               (implicit canvas: ReachCanvas) =
	{
		// Creates the hierarchy block first
		val hierarchy = new SeedHierarchyBlock(canvas)
		// Then creates the component and the wrapper
		new OpenComponent[C, R](creation(hierarchy), hierarchy)
	}
	
	/**
	  * Creates a number of new open components at once. This method should be used only when the components won't
	  * share the same container. If the components will be placed in identical container hierarchies,
	  * apply(...) should be used instead.
	  * @param creation A creation function that accepts an infinite iterator that provides new component hierarchies.
	  *                 Returns the created components, along optional additional results.
	  *                 The number of returned items should match exactly the number of new component hierarchies
	  *                 requested from the iterator. It is not allowed to share a created hierarchy between multiple
	  *                 components.
	  * @param canvas Canvas element that will ultimately host these components (implicit)
	  * @tparam C Type of created components
	  * @return New open components, with additional results included (if defined). Also contains the primary
	  *         additional creation result.
	  */
	def many[C <: ReachComponentLike, CR, R](creation: Iterator[ComponentHierarchy] => CreationsResult[C, CR, R])
					  (implicit canvas: ReachCanvas) =
	{
		// Provides the creation function with an infinite iterator that creates new component hierarchies as requested
		// Collects all created component hierarchies
		val moreHierarchiesIterator = Iterator.continually[ComponentHierarchy] { new SeedHierarchyBlock(canvas) }
		// Wraps the created components
		creation(moreHierarchiesIterator).mapComponent {
			_.iterator.flatMap { component =>
				// Components must use provided seed hierarchies
				component.component.parentHierarchy match {
					case seed: SeedHierarchyBlock => Some(new OpenComponent(component, seed))
					case _ => None
				}
			}.toVector
		}
	}
	
	/**
	  * Creates a new open component
	  * @param factory A factory that produces component factories for target contexts
	  * @param creation Component creation function (returns a component and a possible additional result)
	  * @param canvas Implicit access to top canvas component
	  * @tparam F Type of component creation factory
	  * @tparam C Type of created component
	  * @tparam R Type of additional creation result
	  * @return A new open component
	  */
	def using[F, C, R](factory: ComponentFactoryFactory[F])(creation: F => ComponentCreationResult[C, R])
					  (implicit canvas: ReachCanvas) =
		apply { hierarchy => creation(factory(hierarchy)) }
	
	/**
	  * Creates a number of new open components at once. This method should be used only when the components won't
	  * share the same container. If the components will be placed in identical container hierarchies,
	  * apply(...) should be used instead.
	  * @param factory A factory that produces component factories
	  * @param creation A creation function that accepts an infinite iterator that provides new component factories.
	  *                 Returns the created components, along with pointers that indicate whether those components
	  *                 should be attached to the parent component or not. The number of returned items should match
	  *                 exactly the number of new component hierarchies requested from the iterator. It is not allowed
	  *                 to share a created hierarchy between multiple components.
	  * @param canvas Canvas element that will ultimately host these components (implicit)
	  * @tparam F Type of component factory
	  * @tparam C Type of created components
	  * @return New open components, with their connection pointers as results (if defined)
	  */
	def manyUsing[F, C <: ReachComponentLike, CR, R](factory: ComponentFactoryFactory[F])
							  (creation: Iterator[F] => CreationsResult[C, CR, R])
							  (implicit canvas: ReachCanvas) =
		many[C, CR, R] { hierarchies => creation(hierarchies.map(factory.apply)) }
	
	/**
	  * Creates a new open component using a contextual component factory
	  * @param factory A factory that can produce contextual component factories when specified with the proper context
	  * @param context Component creation context
	  * @param creation Component creation function (accepts a context-specific component creation factory)
	  * @param canvas Implicit access to top canvas component
	  * @tparam C Type of created component
	  * @tparam R Type of additional creation result
	  * @tparam N Type of component creation context
	  * @tparam F Type of context specific component creation factory
	  * @return New component with possible additional creation result
	  */
	@deprecated("Replaced with .withContext(N).apply(...)", "v1.0")
	def withContext[C, R, N, F[X <: N]](factory: Gccff[N, F], context: N)
	                                   (creation: F[N] => ComponentCreationResult[C, R])
	                                   (implicit canvas: ReachCanvas) =
		apply { hierarchy =>
			creation(factory.withContext(hierarchy, context))
		}
	
	/**
	  * Creates a number of new open components at once. This method should be used only when the components won't
	  * share the same container. If the components will be placed in identical container hierarchies,
	  * apply(...) should be used instead.
	  * @param factory A factory that produces context-aware component factories
	  * @param context Component creation context used in the produced factories
	  * @param creation A creation function that accepts an infinite iterator that provides new component factories.
	  *                 Returns the created components, along with pointers that indicate whether those components
	  *                 should be attached to the parent component or not. The number of returned items should match
	  *                 exactly the number of new component hierarchies requested from the iterator. It is not allowed
	  *                 to share a created hierarchy between multiple components.
	  * @param canvas Canvas element that will ultimately host these components (implicit)
	  * @tparam C Type of created components
	  * @tparam N Type of component creation context
	  * @tparam F Type of component creation factory
	  * @return New open components, with their connection pointers as results (if defined)
	  */
	@deprecated("Replaced with .withContext(N).many(...)", "v1.0")
	def manyWithContext[C <: ReachComponentLike, CR, R, N, F[X <: N]](factory: Gccff[N, F], context: N)
	                                                                 (creation: Iterator[F[N]] => CreationsResult[C, CR, R])
	                                                                 (implicit canvas: ReachCanvas) =
		many { hierarchies => creation(hierarchies.map { factory.withContext(_, context) }) }
	
	/**
	  * Creates a new open component using a contextual component factory
	  * @param factory A factory that can produce contextual component factories when specified with the proper context
	  * @param creation Component creation function (accepts a context-specific component creation factory)
	  * @param canvas Implicit access to top canvas component
	  * @param context Implicit component creation context
	  * @tparam C Type of created component
	  * @tparam R Type of additional creation result
	  * @tparam N Type of component creation context
	  * @tparam F Type of context specific component creation factory
	  * @return New component with possible additional creation result
	  */
	@deprecated("Replaced with .contextual.apply(...)", "v1.0")
	def contextual[C, R, N, F[X <: N]](factory: Gccff[N, F])(creation: F[N] => ComponentCreationResult[C, R])
	                                  (implicit canvas: ReachCanvas, context: N) =
		withContext(factory, context)(creation)
	
	/**
	  * Creates a number of new open components at once. This method should be used only when the components won't
	  * share the same container. If the components will be placed in identical container hierarchies,
	  * apply(...) should be used instead.
	  * @param factory A factory that produces context-aware component factories
	  * @param creation A creation function that accepts an infinite iterator that provides new component hierarchies.
	  *                 Returns the created components, along with pointers that indicate whether those components
	  *                 should be attached to the parent component or not. The number of returned items should match
	  *                 exactly the number of new component hierarchies requested from the iterator. It is not allowed
	  *                 to share a created hierarchy between multiple components.
	  * @param canvas Canvas element that will ultimately host these components (implicit)
	  * @param context Component creation context used in the produced factories
	  * @tparam C Type of created components
	  * @tparam N Type of component creation context
	  * @tparam F Type of component creation factory
	  * @return New open components, with their connection pointers as results (if defined)
	  */
	@deprecated("Replaced with .contextual.many(...)", "v1.0")
	def contextualMany[C <: ReachComponentLike, CR, R, N, F[X <: N]](factory: Gccff[N, F])
	                                                                (creation: Iterator[F[N]] => CreationsResult[C, CR, R])
	                                                                (implicit canvas: ReachCanvas, context: N) =
		manyWithContext(factory, context)(creation)
}

case class ContextualOpenComponentFactory[N](context: N)
{
	/**
	  * Creates a new open component using a contextual component factory
	  * @param factory  A factory that can produce contextual component factories when specified with the proper context
	  * @param creation Component creation function (accepts a context-specific component creation factory)
	  * @param canvas   Implicit access to top canvas component
	  * @tparam F Type of context specific component creation factory
	  * @tparam C Type of created component
	  * @tparam R Type of additional creation result
	  * @return New component with possible additional creation result
	  */
	def apply[F[X <: N], C, R](factory: Gccff[N, F])(creation: F[N] => ComponentCreationResult[C, R])
	                          (implicit canvas: ReachCanvas) =
		Open { hierarchy => creation(factory.withContext(hierarchy, context)) }
	
	/**
	  * Creates a new open component using a contextual component factory
	  * @param factory  A factory that can produce contextual component factories when specified with the proper context
	  * @param creation Component creation function (accepts a contextual component creation factory)
	  * @param canvas   Implicit access to top canvas component
	  * @tparam F  Type of context specific component creation factory
	  * @tparam C  Type of created component
	  * @tparam R  Type of additional creation result
	  * @return New component with possible additional creation result
	  */
	def apply[F, C, R](factory: Ccff[N, F])(creation: F => ComponentCreationResult[C, R])
	                  (implicit canvas: ReachCanvas) =
		Open { hierarchy => creation(factory.withContext(hierarchy, context)) }
	
	/**
	  * Creates a number of new open components at once. This method should be used only when the components won't
	  * share the same container. If the components will be placed in identical container hierarchies,
	  * apply(...) should be used instead.
	  * @param factory  A factory that produces context-aware component factories
	  * @param creation A creation function that accepts an infinite iterator that provides new component factories.
	  *                 Returns the created components, along with pointers that indicate whether those components
	  *                 should be attached to the parent component or not. The number of returned items should match
	  *                 exactly the number of new component hierarchies requested from the iterator. It is not allowed
	  *                 to share a created hierarchy between multiple components.
	  * @param canvas   Canvas element that will ultimately host these components (implicit)
	  * @tparam F  Type of component creation factory
	  * @tparam C  Type of created components
	  * @tparam CR Additional creation result type for individual components
	  * @tparam R  Additional (reduced) creation result type
	  * @return New open components, with their connection pointers as results (if defined)
	  */
	def many[F[X <: N], C <: ReachComponentLike, CR, R](factory: Gccff[N, F])
	                                                   (creation: Iterator[F[N]] => CreationsResult[C, CR, R])
	                                                   (implicit canvas: ReachCanvas) =
		Open.many { hierarchies => creation(hierarchies.map { factory.withContext(_, context) }) }
	
	/**
	  * Creates a number of new open components at once. This method should be used only when the components won't
	  * share the same container. If the components will be placed in identical container hierarchies,
	  * apply(...) should be used instead.
	  * @param factory  A factory that produces context-aware component factories
	  * @param creation A creation function that accepts an infinite iterator that provides new component factories.
	  *                 Returns the created components, along with pointers that indicate whether those components
	  *                 should be attached to the parent component or not. The number of returned items should match
	  *                 exactly the number of new component hierarchies requested from the iterator. It is not allowed
	  *                 to share a created hierarchy between multiple components.
	  * @param canvas   Canvas element that will ultimately host these components (implicit)
	  * @tparam F  Type of component creation factory
	  * @tparam C  Type of created components
	  * @tparam CR Additional creation result type for individual components
	  * @tparam R  Additional (reduced) creation result type
	  * @return New open components, with their connection pointers as results (if defined)
	  */
	def many[F, C <: ReachComponentLike, CR, R](factory: FromContextComponentFactoryFactory[N, F])
	                                           (creation: Iterator[F] => CreationsResult[C, CR, R])
	                                           (implicit canvas: ReachCanvas) =
		Open.many { hierarchies => creation(hierarchies.map { factory.withContext(_, context) }) }
	
}

object OpenComponent
{
	// TYPES    -----------------------------
	
	/**
	  * Type of open component that wraps multiple components
	  */
	type BundledOpenComponents[+C, +R] = OpenComponent[Seq[C], R]
	/**
	  * Type for separate open components
	  */
	type SeparateOpenComponents[+C, +R] = Seq[OpenComponent[C, R]]
	/**
	  * Type for separate open components that may be switched on or off using a specific pointer.
	  * Typically used in view containers.
	  */
	type SwitchableOpenComponents[+C, +R] = ComponentCreationResult[SeparateOpenComponents[C, Changing[Boolean]], R]
	/**
	  * Type of open component that wraps a main component, plus possibly multiple additional layers.
	  */
	type OpenLayerComponents[+M, +C, +R] = OpenComponent[(M, Seq[(C, LayerPositioning)]), R]
	
	
	// IMPLICIT	-----------------------------
	
	// Allows one to implicitly access the wrapped component
	implicit def autoAccessComponent[C](open: OpenComponent[C, _]): C = open.component
	
	
	// EXTENSIONS	-------------------------
	
	// Extensions for single wrapped components
	implicit class SingleOpenComponent[C <: ReachComponentLike, R](val c: OpenComponent[C, R]) extends AnyVal
	{
		/**
		  * A framed version of this component
		  * @param insets Insets to be placed around this component
		  * @param customDrawers Custom drawers to apply to this component
		  * @return A new framing with this component inside it (contains the same custom result as this one)
		  */
		def framed(insets: StackInsetsConvertible, customDrawers: Seq[CustomDrawer] = Empty) =
		{
			Open.using(Framing) { ff =>
				val wrapping = ff.apply(insets).withCustomDrawers(customDrawers)(c)
				wrapping.parent -> wrapping.result
			}(c.parentHierarchy.top)
		}
		
		/**
		  * A framed version of this component
		  * @param insets Insets to be placed around this component
		  * @param backgroundColor Color used as the framing's background color
		  * @return A new framing with this component inside it (contains the same custom result as this one)
		  */
		def framed(insets: StackInsetsConvertible, backgroundColor: Color): OpenComponent[Framing, R] =
			framed(insets, Single(BackgroundDrawer(backgroundColor)))
	}
	
	// Extension for sequence of wrapped components
	implicit class MultiOpenComponent[C <: ReachComponentLike, R](val c: OpenComponent[Seq[C], R]) extends AnyVal
	{
		/**
		  * Creates a stack that will hold these components
		  * @param direction Axis along which the components are stacked / form a line (default = Y = column)
		  * @param layout Layout used for handling lengths perpendicular to stack direction (breadth)
		  *                  (default = Fit = All components have same breadth as this stack)
		  * @param cap Cap placed at each end of this stack (default = always 0)
		  * @param customDrawers Custom drawers attached to this stack (default = empty)
		  * @param areRelated Whether the components should be considered closely related (uses smaller margin)
		  *                  (default = false)
		  * @param context Implicit component creation context
		  * @param canvas A set of reach canvases to hold these components
		  * @return A new stack with these components inside. Also contains the same additional creation result
		  *         as this one.
		  */
		def stack(direction: Axis2D = Y, layout: StackLayout = Fit, cap: StackLength = StackLength.fixedZero,
				  customDrawers: Seq[CustomDrawer] = Empty, areRelated: Boolean = false)
			   (implicit context: BaseContext, canvas: ReachCanvas) =
			Open.withContext(context).apply(Stack) { sf =>
				val settings = StackSettings(axis = direction, layout = layout, cap = cap, customDrawers = customDrawers)
				val stack = sf.copy(settings = settings, areRelated = areRelated)(c)
				stack.parent -> stack.result
			}
		
		/**
		  * Creates a stack row that will hold these components
		  * @param layout Layout used for handling lengths perpendicular to stack direction (breadth)
		  *                  (default = Fit = All components have same breadth as this stack)
		  * @param cap Cap placed at each end of this stack (default = always 0)
		  * @param customDrawers Custom drawers attached to this stack (default = empty)
		  * @param areRelated Whether the components should be considered closely related (uses smaller margin)
		  *                  (default = false)
		  * @param context Implicit component creation context
		  * @param canvas A set of reach canvases to hold these components
		  * @return A new stack with these components inside. Also contains the same additional creation result
		  *         as this one.
		  */
		def row(layout: StackLayout = Fit, cap: StackLength = StackLength.fixedZero,
				  customDrawers: Seq[CustomDrawer] = Empty, areRelated: Boolean = false)
				 (implicit context: BaseContext, canvas: ReachCanvas) =
			stack(X, layout, cap, customDrawers, areRelated)
		
		/**
		  * Creates a stack column that will hold these components
		  * @param layout Layout used for handling lengths perpendicular to stack direction (breadth)
		  *                  (default = Fit = All components have same breadth as this stack)
		  * @param cap Cap placed at each end of this stack (default = always 0)
		  * @param customDrawers Custom drawers attached to this stack (default = empty)
		  * @param areRelated Whether the components should be considered closely related (uses smaller margin)
		  *                  (default = false)
		  * @param context Implicit component creation context
		  * @param canvas A set of reach canvases to hold these components
		  * @return A new stack with these components inside. Also contains the same additional creation result
		  *         as this one.
		  */
		def column(layout: StackLayout = Fit, cap: StackLength = StackLength.fixedZero,
				customDrawers: Seq[CustomDrawer] = Empty, areRelated: Boolean = false)
			   (implicit context: BaseContext, canvas: ReachCanvas) =
			stack(Y, layout, cap, customDrawers, areRelated)
	}
}

/**
  * A wrapper that contains a component with an incomplete stack hierarchy. Open components are then completed and
  * closed by placing them inside wrappers or containers
  * @author Mikko Hilpinen
  * @since 11.10.2020, v0.1
  */
class OpenComponent[+C, +R](val creation: ComponentCreationResult[C, R], val hierarchy: SeedHierarchyBlock)
{
	// COMPUTED	---------------------------------
	
	/**
	  * @return The wrapped component
	  */
	def component = creation.component
	/**
	  * @return Additional component creation result value
	  */
	def result = creation.result
	
	/**
	  * @return A tuple containing the wrapped component and the wrapped additional result,
	  *         but not the component hierarchy
	  */
	def componentAndResult = component -> result
	
	
	// OTHER	---------------------------------
	
	/**
	  * Attaches this component to a parent container
	  * @param parent A parent container
	  * @param switchPointer A pointer to the changing attachment status (default = always attached)
	  * @tparam P Type of parent container
	  * @throws IllegalStateException if this component was already attached to a parent container
	  * @return A result with the wrapping parent container, the wrapped component and a component creation result
	  */
	@throws[IllegalStateException]("If this component was already attached to a parent container")
	def attachTo[P <: ReachComponentLike](parent: P, switchPointer: Changing[Boolean] = AlwaysTrue) =
	{
		hierarchy.complete(parent, switchPointer)
		creation.in(parent)
	}
	/**
	  * Attaches this component to a custom component hierarchy
	  * @param parent Parent container
	  * @param hierarchy Component hierarchy block that will replace the seed used by this open component
	  * @tparam P Type of the parent container
	  * @return A result wrapping the parent container, the wrapped component and a component creation result
	  */
	def attachTo[P](parent: P, hierarchy: ComponentHierarchy) = {
		this.hierarchy.replaceWith(hierarchy)
		creation.in(parent)
	}
	
	/**
	  * @param f A mapping function for the component part
	  * @tparam C2 Mapping function result
	  * @return A copy of this component with mapped component
	  */
	def mapComponent[C2](f: C => C2) = new OpenComponent(creation.mapComponent(f), hierarchy)
	
	/**
	  * @param newResult New additional result
	  * @tparam R2 Type of the new result
	  * @return A copy of this component with the new additional result
	  */
	def withResult[R2](newResult: R2) = new OpenComponent(creation.withResult(newResult), hierarchy)
	/**
	  * @param f Result mapping function
	  * @tparam R2 Type of the new result
	  * @return A copy of this component with mapped additional result
	  */
	def mapResult[R2](f: R => R2) = new OpenComponent(creation.mapResult(f), hierarchy)
}