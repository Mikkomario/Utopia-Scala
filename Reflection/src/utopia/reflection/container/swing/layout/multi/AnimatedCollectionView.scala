package utopia.reflection.container.swing.layout.multi

import utopia.firmament.context.{AnimationContext, BaseContext, ComponentCreationDefaults}
import utopia.firmament.drawing.mutable.MutableCustomDrawableWrapper
import utopia.firmament.model.enumeration.StackLayout
import utopia.firmament.model.enumeration.StackLayout.{Fit, Leading}
import utopia.genesis.handling.mutable.ActorHandler
import utopia.genesis.util.Fps
import utopia.paradigm.enumeration.Axis2D
import utopia.reflection.component.swing.template.{StackableAwtComponentWrapperWrapper, SwingComponentRelated}
import utopia.reflection.container.stack.template.layout.ReflectionCollectionViewLike
import utopia.reflection.container.swing.AwtContainerRelated
import utopia.reflection.container.swing.layout.multi.Stack.AwtStackable
import utopia.firmament.model.stack.StackLength

import scala.concurrent.ExecutionContext
import scala.concurrent.duration.FiniteDuration

object AnimatedCollectionView
{
	/**
	  * Creates a new animated collection view using component creation context
	  * @param rowAxis Axis of rows in this collection (the first axis on which items are added)
	  * @param initialRowSplitThreshold A pixel threshold after which a row is split into two
	  * @param insideRowLayout Stack layout used inside a row (default = Fit)
	  * @param forceEqualRowLength Whether all rows should have equal length (default = false)
	  * @param ac Animation context (implicit)
	  * @param bc Base context (implicit)
	  * @param exc Execution context (implicit)
	  * @tparam C Type of component in this container
	  * @return A new collection view
	  */
	def contextual[C <: AwtStackable](rowAxis: Axis2D, initialRowSplitThreshold: Double,
									  insideRowLayout: StackLayout = Fit, forceEqualRowLength: Boolean = false)
									 (implicit ac: AnimationContext, bc: BaseContext, exc: ExecutionContext) =
		new AnimatedCollectionView[C](ac.actorHandler, rowAxis, initialRowSplitThreshold, bc.stackMargin,
			insideRowLayout, forceEqualRowLength, ac.animationDuration, ac.maxAnimationRefreshRate,
			ac.useFadingInAnimations)
}

/**
 * This container places items in rows and columns, filling a 2D space. All item additions are animated.
 * @author Mikko Hilpinen
 * @since 24.4.2020, v1.2
 */
class AnimatedCollectionView[C <: AwtStackable](actorHandler: ActorHandler, rowAxis: Axis2D, initialRowSplitThreshold: Double,
												margin: StackLength = StackLength.any, insideRowLayout: StackLayout = Fit,
												forceEqualRowLength: Boolean = false,
												animationDuration: FiniteDuration = ComponentCreationDefaults.transitionDuration,
											   maxAnimationRefreshRate: Fps = ComponentCreationDefaults.maxAnimationRefreshRate,
												useFadingInAnimations: Boolean = true)(implicit exc: ExecutionContext)
	extends ReflectionCollectionViewLike[C, AnimatedStack[C], AnimatedStack[AnimatedStack[C]]] with StackableAwtComponentWrapperWrapper
		with SwingComponentRelated with AwtContainerRelated with MutableCustomDrawableWrapper
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
			animationDuration = animationDuration, maxRefreshRate = maxAnimationRefreshRate,
			fadingIsEnabled = useFadingInAnimations)
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
