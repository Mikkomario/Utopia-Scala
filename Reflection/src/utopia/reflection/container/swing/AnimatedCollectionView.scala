package utopia.reflection.container.swing

import utopia.flow.util.TimeExtensions._
import utopia.genesis.handling.mutable.ActorHandler
import utopia.genesis.shape.Axis2D
import utopia.reflection.component.drawing.mutable.CustomDrawableWrapper
import utopia.reflection.component.swing.{StackableAwtComponentWrapperWrapper, SwingComponentRelated}
import utopia.reflection.container.stack.StackLayout.{Fit, Leading}
import utopia.reflection.container.stack.{CollectionViewLike, StackLayout}
import utopia.reflection.container.swing.Stack.AwtStackable
import utopia.reflection.shape.StackLength

import scala.concurrent.ExecutionContext
import scala.concurrent.duration.FiniteDuration

/**
 * This container places items in rows and columns, filling a 2D space. All item additions are animated.
 * @author Mikko Hilpinen
 * @since 24.4.2020, v1.2
 */
class AnimatedCollectionView[C <: AwtStackable](actorHandler: ActorHandler, rowAxis: Axis2D, initialRowSplitThreshold: Double,
												margin: StackLength = StackLength.any, insideRowLayout: StackLayout = Fit,
												forceEqualRowLength: Boolean = false,
												animationDuration: FiniteDuration = 0.25.seconds,
												useFadingInAnimations: Boolean = true)(implicit exc: ExecutionContext)
	extends CollectionViewLike[C, AnimatedStack[C], AnimatedStack[AnimatedStack[C]]] with StackableAwtComponentWrapperWrapper
		with SwingComponentRelated with AwtContainerRelated with CustomDrawableWrapper
{
	// ATTRIBUTES	-----------------------
	
	protected val container =
	{
		val layout =
		{
			if (forceEqualRowLength)
				Fit
			else
				Leading
		}
		new AnimatedStack[AnimatedStack[C]](actorHandler, rowAxis.perpendicular, margin, layout = layout,
			animationDuration = animationDuration, fadingIsEnabled = useFadingInAnimations)
	}
	
	private var _rowSplitThreshold = initialRowSplitThreshold
	
	
	// COMPUTED	---------------------------
	
	/**
	 * @return The length threshold at which point a new row is started
	 */
	def rowSplitThreshold = _rowSplitThreshold
	def rowSplitThreshold_=(newThreshold: Double) =
	{
		_rowSplitThreshold = newThreshold
		revalidate()
	}
	
	
	// IMPLEMENTED	-----------------------
	
	override def component = container.component
	
	override protected def collectionMaxCapacity = rowSplitThreshold
	
	override protected def spaceOf(component: C) = component.stackSize.along(rowAxis).optimal
	
	override protected def betweenComponentsSpace = margin.optimal
	
	override protected def newCollection() = new AnimatedStack[C](actorHandler, rowAxis, margin,
		layout = insideRowLayout, animationDuration = animationDuration, fadingIsEnabled = useFadingInAnimations)
	
	override def drawable = container
}
