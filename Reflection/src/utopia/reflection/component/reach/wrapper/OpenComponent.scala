package utopia.reflection.component.reach.wrapper

import scala.language.implicitConversions
import utopia.flow.event.Changing
import utopia.genesis.color.Color
import utopia.genesis.shape.Axis.{X, Y}
import utopia.genesis.shape.Axis2D
import utopia.reflection.component.context.BaseContextLike
import utopia.reflection.component.drawing.immutable.BackgroundDrawer
import utopia.reflection.component.drawing.template.CustomDrawer
import utopia.reflection.component.reach.hierarchy.{ComponentHierarchy, SeedHierarchyBlock}
import utopia.reflection.component.reach.template.ReachComponentLike
import utopia.reflection.container.reach.{Framing, Stack}
import utopia.reflection.container.stack.StackLayout
import utopia.reflection.container.stack.StackLayout.Fit
import utopia.reflection.container.swing.ReachCanvas
import utopia.reflection.shape.stack.{StackInsetsConvertible, StackLength}

object OpenComponent
{
	// IMPLICIT	-----------------------------
	
	// Allows one to implicitly access the wrapped component
	implicit def autoAccessComponent[C](open: OpenComponent[C, _]): C = open.component
	
	
	// OTHER	-----------------------------
	
	/**
	  * Creates a new open component
	  * @param creation Component creation function (returns a component and a possible additional result)
	  * @param canvas Implicit access to top canvas component
	  * @tparam C Type of created component
	  * @return A new open component
	  */
	def apply[C, R](creation: ComponentHierarchy => ComponentCreationResult[C, R])(implicit canvas: ReachCanvas) =
	{
		// Creates the hierarchy block first
		val hierarchy = new SeedHierarchyBlock(canvas)
		// Then creates the component and the wrapper
		new OpenComponent[C, R](creation(hierarchy), hierarchy)
	}
	
	
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
			apply { hierarchy =>
				val wrapping = Framing(hierarchy, c, insets, customDrawers)
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
			apply { hierarchy =>
				val stack = Stack(hierarchy, c, direction, layout, cap, customDrawers, areRelated)
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