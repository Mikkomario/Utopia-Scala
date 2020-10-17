package utopia.reflection.component.reach.wrapper

import scala.language.implicitConversions
import utopia.flow.event.Changing
import utopia.genesis.color.Color
import utopia.genesis.shape.Axis.{X, Y}
import utopia.genesis.shape.Axis2D
import utopia.reflection.component.context.BaseContextLike
import utopia.reflection.component.drawing.immutable.BackgroundDrawer
import utopia.reflection.component.drawing.template.CustomDrawer
import utopia.reflection.component.reach.factory.{ComponentFactoryFactory, ContextInsertableComponentFactoryFactory, ContextualComponentFactory}
import utopia.reflection.component.reach.hierarchy.{ComponentHierarchy, SeedHierarchyBlock}
import utopia.reflection.component.reach.template.ReachComponentLike
import utopia.reflection.container.reach.{Framing, Stack}
import utopia.reflection.container.stack.StackLayout
import utopia.reflection.container.stack.StackLayout.Fit
import utopia.reflection.container.swing.ReachCanvas
import utopia.reflection.shape.stack.{StackInsetsConvertible, StackLength}

object Open
{
	/**
	  * Creates a new open component
	  * @param creation Component creation function (returns a component and a possible additional result)
	  * @param canvas Implicit access to top canvas component
	  * @tparam C Type of created component
	  * @tparam R Type of additional creation result
	  * @return A new open component
	  */
	def apply[C, R](creation: ComponentHierarchy => ComponentCreationResult[C, R])(implicit canvas: ReachCanvas) =
	{
		// Creates the hierarchy block first
		val hierarchy = new SeedHierarchyBlock(canvas)
		// Then creates the component and the wrapper
		new OpenComponent[C, R](creation(hierarchy), hierarchy)
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
	// FIXME: This method doesn't work without specifying all generic parameter types
	def withContext[C, R, N, F[X <: N] <: ContextualComponentFactory[X, _ >: N, F]]
	(factory: ContextInsertableComponentFactoryFactory[_ >: N, _, F], context: N)
	(creation: F[N] => ComponentCreationResult[C, R])(implicit canvas: ReachCanvas) =
	{
		apply { hierarchy =>
			creation(factory.withContext(hierarchy, context))
		}
	}
	
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
	def contextual[C, R, N, F[X <: N] <: ContextualComponentFactory[X, _ >: N, F]]
	(factory: ContextInsertableComponentFactoryFactory[_ >: N, _, F])(creation: F[N] => ComponentCreationResult[C, R])
	(implicit canvas: ReachCanvas, context: N) =
		withContext(factory, context)(creation)
}

object OpenComponent
{
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
		def framed(insets: StackInsetsConvertible, customDrawers: Vector[CustomDrawer] = Vector()) =
		{
			Open.using(Framing) { ff =>
				val wrapping = ff(c, insets, customDrawers)
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
			framed(insets, Vector(new BackgroundDrawer(backgroundColor)))
	}
	
	// Extension for sequence of wrapped components
	implicit class MultiOpenComponent[C <: ReachComponentLike, R](val c: OpenComponent[Vector[C], R]) extends AnyVal
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
				  customDrawers: Vector[CustomDrawer] = Vector(), areRelated: Boolean = false)
			   (implicit context: BaseContextLike, canvas: ReachCanvas) =
			Open.withContext(Stack, context) { sf =>
				val stack = sf(c, direction, layout, cap, customDrawers, areRelated)
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
				  customDrawers: Vector[CustomDrawer] = Vector(), areRelated: Boolean = false)
				 (implicit context: BaseContextLike, canvas: ReachCanvas) =
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
				customDrawers: Vector[CustomDrawer] = Vector(), areRelated: Boolean = false)
			   (implicit context: BaseContextLike, canvas: ReachCanvas) =
			stack(Y, layout, cap, customDrawers, areRelated)
	}
}

/**
  * A wrapper that contains a component with an incomplete stack hierarchy. Open components are then completed and
  * closed by placing them inside wrappers or containers
  * @author Mikko Hilpinen
  * @since 11.10.2020, v2
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
	
	
	// OTHER	---------------------------------
	
	/**
	  * Attaches this component to a parent container
	  * @param parent A parent container
	  * @param switchPointer A pointer to the changing attachment status (optional)
	  * @tparam P Type of parent container
	  * @throws IllegalStateException if this component was already attached to a parent container
	  * @return A result with the wrapping parent container, the wrapped component and component creation result
	  */
	@throws[IllegalStateException]
	def attachTo[P <: ReachComponentLike](parent: P, switchPointer: Option[Changing[Boolean]] = None) =
	{
		hierarchy.complete(parent, switchPointer)
		creation.in(parent)
	}
}