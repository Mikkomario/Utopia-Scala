package utopia.reach.container.wrapper

import utopia.firmament.component.container.single.SingleContainer
import utopia.firmament.drawing.immutable.CustomDrawableFactory
import utopia.firmament.drawing.template.CustomDrawer
import utopia.firmament.model.CoordinateTransform
import utopia.firmament.model.stack.StackSize
import utopia.flow.collection.immutable.Empty
import utopia.flow.view.template.eventful.Flag
import utopia.genesis.graphics.DrawLevel.{Background, Foreground, Normal}
import utopia.genesis.graphics.Drawer
import utopia.genesis.handling.event.mouse.MouseEvent
import utopia.paradigm.angular.Rotation
import utopia.paradigm.enumeration.Axis.{X, Y}
import utopia.paradigm.enumeration.RotationDirection
import utopia.paradigm.enumeration.RotationDirection.{Clockwise, Counterclockwise}
import utopia.paradigm.shape.shape2d.Matrix2D
import utopia.paradigm.shape.shape2d.area.polygon.c4.bounds.Bounds
import utopia.paradigm.shape.shape2d.vector.point.Point
import utopia.paradigm.shape.shape2d.vector.size.Size
import utopia.paradigm.shape.template.vector.DoubleVectorLike
import utopia.paradigm.transform.LinearTransformation
import utopia.reach.component.factory.ComponentFactoryFactory.Cff
import utopia.reach.component.factory.FromGenericContextFactory
import utopia.reach.component.hierarchy.ComponentHierarchy
import utopia.reach.component.template.{ConcreteCustomDrawReachComponent, ReachComponent}
import utopia.reach.component.wrapper.{ComponentWrapResult, OpenComponent}
import utopia.reach.container.ReachCanvas

trait RotatedFactoryLike[+Repr]
	extends WrapperContainerFactory[Rotated, ReachComponent] with CustomDrawableFactory[Repr]
{
	// ABSTRACT ----------------------
	
	/**
	  * @return The direction to which the contents are rotated
	  */
	def direction: RotationDirection
	
	/**
	  * @param direction Direction to which the wrapped component is rotated
	  * @return Copy of this factory using the specified rotation direction
	  */
	def withDirection(direction: RotationDirection): Repr
	
	
	// COMPUTED -----------------------------
	
	/**
	  * @return Copy of this factory that rotates clockwise
	  */
	def clockwise = withDirection(Clockwise)
	/**
	  * @return Copy of this factory that rotates counter-clockwise
	  */
	def counterClockwise = withDirection(Counterclockwise)
	
	
	// IMPLEMENTED  -------------------------
	
	override def apply[C <: ReachComponent, R](content: OpenComponent[C, R]): ComponentWrapResult[Rotated, C, R] = {
		val container = new Rotated(hierarchy, content.component, direction, customDrawers)
		content.attachTo(container, RotatedHierarchy(hierarchy, container, direction))
	}
}

case class RotatedFactory(hierarchy: ComponentHierarchy, direction: RotationDirection = Clockwise,
                          customDrawers: Seq[CustomDrawer] = Empty)
	extends RotatedFactoryLike[RotatedFactory] with NonContextualWrapperContainerFactory[Rotated, ReachComponent]
		with FromGenericContextFactory[Any, ContextualRotatedFactory]
{
	override def withCustomDrawers(drawers: Seq[CustomDrawer]): RotatedFactory = copy(customDrawers = customDrawers)
	override def withDirection(direction: RotationDirection): RotatedFactory = copy(direction = direction)
	
	override def withContext[N <: Any](context: N): ContextualRotatedFactory[N] =
		ContextualRotatedFactory(hierarchy, context, direction, customDrawers)
}

case class ContextualRotatedFactory[+N](hierarchy: ComponentHierarchy, context: N,
                                        direction: RotationDirection = Clockwise,
                                        customDrawers: Seq[CustomDrawer] = Empty)
	extends RotatedFactoryLike[ContextualRotatedFactory[N]]
		with ContextualWrapperContainerFactory[N, Any, Rotated, ReachComponent, ContextualRotatedFactory]
{
	override def withContext[N2 <: Any](newContext: N2): ContextualRotatedFactory[N2] = copy(context = newContext)
	override def withCustomDrawers(drawers: Seq[CustomDrawer]): ContextualRotatedFactory[N] =
		copy(customDrawers = drawers)
	override def withDirection(direction: RotationDirection): ContextualRotatedFactory[N] = copy(direction = direction)
}

// A component hierarchy block which performs the necessary coordinate transformations
private case class RotatedHierarchy(parentHierarchy: ComponentHierarchy, container: ReachComponent,
                                    direction: RotationDirection)
	extends ComponentHierarchy
{
	// ATTRIBUTES   --------------------
	
	override val parent: Either[ReachCanvas, (ComponentHierarchy, ReachComponent)] =
		Right(parentHierarchy -> container)
	
	private val rotation = Matrix2D.quarterRotationTowards(direction)
	private val translatePointer = direction match {
		case Clockwise => container.sizePointer.map { s => X(s.width) }
		case Counterclockwise => container.sizePointer.map { s => Y(s.height) }
	}
	
	
	// IMPLEMENTED  --------------------
	
	override def linkedFlag: Flag = parentHierarchy.linkedFlag
	override def isThisLevelLinked: Boolean = true
	
	override def coordinateTransform: Option[CoordinateTransform] = Some(Transform)
	
	
	// NESTED   ------------------------
	
	private object Transform extends CoordinateTransform
	{
		override def apply[V <: DoubleVectorLike[V]](coordinate: V): V =
			(coordinate - container.position - translatePointer.value) * rotation
		
		override def invert[V <: DoubleVectorLike[V]](coordinate: V): V =
			(coordinate * rotation) + translatePointer.value
		
		override def invert(area: Bounds): Bounds =
			Bounds.between(invert(area.topLeft), invert(area.bottomRight))
	}
}

object Rotated extends Cff[RotatedFactory]
{
	// IMPLEMENTED  ------------------------
	
	override def apply(hierarchy: ComponentHierarchy): RotatedFactory = RotatedFactory(hierarchy)
}

/**
  * A container that rotates the wrapped component 90 degrees clockwise or counter-clockwise.
  * @author Mikko Hilpinen
  * @since 17.08.2024, v1.4
  */
class Rotated(override val hierarchy: ComponentHierarchy, override val content: ReachComponent,
              direction: RotationDirection = Clockwise,
              override val customDrawers: Seq[CustomDrawer] = Empty)
	extends ConcreteCustomDrawReachComponent with SingleContainer[ReachComponent]
{
	// ATTRIBUTES   ---------------------------
	
	private val rotation = LinearTransformation.rotation(Rotation.quarter.towards(direction))
	private val inverseRotation = -rotation
	
	private val translationPointer = direction match {
		case Clockwise => sizePointer.map { s => X(s.width) }
		case Counterclockwise => sizePointer.map { s => Y(s.height) }
	}
	private val transformPointer = translationPointer.map(rotation.withTranslation)
	
	
	// IMPLEMENTED  ---------------------------
	
	override def calculatedStackSize: StackSize = {
		val original = content.stackSize
		// Flips the wrapped component's stack size
		original.y x original.x
	}
	
	override def updateLayout(): Unit = {
		// Updates child content size by setting it to this component's size, except rotated
		val mySize = size
		content.size = Size(mySize.height, mySize.width)
	}
	
	override def paintWith(drawer: Drawer, clipZone: Option[Bounds]) = {
		// Paints background & normal layers normally
		paintContent(drawer, Background, clipZone)
		paintContent(drawer, Normal, clipZone)
		
		// Paints the content using a transformed drawer & clip zone
		val transform = transformPointer.value
		val childClipZone = clipZone.map { b => Bounds.between(relativize(b.topLeft), relativize(b.bottomRight)) }
		content.paintWith(drawer.translated(position) * transform, childClipZone)
		
		paintContent(drawer, Foreground, clipZone)
	}
	
	override protected def relativizeMouseEventForChildren[E](event: MouseEvent[E]) =
		event.mapPosition { _.mapRelative(relativize) }
		
	
	// OTHER    ----------------------------
	
	// Transforms a coordinate from this component's coordinate system to that of the rotated content
	private def relativize(p: Point) = (p - position - translationPointer.value) * inverseRotation
}
