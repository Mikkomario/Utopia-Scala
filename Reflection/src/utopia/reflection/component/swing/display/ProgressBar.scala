package utopia.reflection.component.swing.display

import utopia.flow.async.VolatileFlag
import utopia.flow.event.{ChangeEvent, ChangeListener, ChangingLike}
import utopia.genesis.animation.Animation
import utopia.genesis.color.Color
import utopia.genesis.handling.Actor
import utopia.genesis.handling.mutable.ActorHandler
import utopia.genesis.shape.Axis.X
import utopia.genesis.shape.shape2D.Bounds
import utopia.genesis.util.Drawer
import utopia.inception.handling.immutable.Handleable
import utopia.reflection.component.context.{AnimationContextLike, ColorContextLike}
import utopia.reflection.component.drawing.mutable.CustomDrawableWrapper
import utopia.reflection.component.drawing.template.CustomDrawer
import utopia.reflection.component.drawing.template.DrawLevel.Normal
import utopia.reflection.component.swing.StackSpace
import utopia.reflection.component.swing.template.SwingComponentRelated
import utopia.reflection.component.template.layout.stack.StackableWrapper
import utopia.reflection.shape.stack.StackSize
import utopia.reflection.util.ComponentCreationDefaults

import scala.concurrent.ExecutionContext
import scala.concurrent.duration.FiniteDuration

object ProgressBar
{
	/**
	  * Creates a new progress bar utilizing component creation context
	  * @param stackSize Progress bar size
	  * @param progressPointer Progress pointer
	  * @param context Component creation context with color information (implicit)
	  * @param animationContext Component creation context for animations (implicit)
	  * @return A new progress bar
	  */
	def contextual(stackSize: StackSize, progressPointer: ChangingLike[Double])(implicit context: ColorContextLike,
																				animationContext: AnimationContextLike) =
		new ProgressBar(animationContext.actorHandler, stackSize,
			context.colorScheme.gray.forBackground(context.containerBackground),
			context.colorScheme.secondary.forBackground(context.containerBackground), progressPointer,
			animationContext.animationDuration)
}

/**
  * Used for displaying progress of some longer operation
  * @author Mikko Hilpinen
  * @since 1.8.2019, v1+
  */
class ProgressBar(actorHandler: ActorHandler, _stackSize: StackSize, val backgroundColor: Color, val barColor: Color,
				  progressPointer: ChangingLike[Double],
				  animationDuration: FiniteDuration = ComponentCreationDefaults.transitionDuration)
	extends StackableWrapper with CustomDrawableWrapper with SwingComponentRelated
{
	// ATTRIBUTES	---------------------
	
	private val label = StackSpace.drawingWith(ProgressDrawer, _stackSize)
	
	// Used for holding visible completion status, can be listened
	private val isCompletedFlag = new VolatileFlag(progress >= 1)
	
	
	// INITIAL CODE ----------------------
	
	progressPointer.addListenerAndSimulateEvent(0.0)(InvisibleProgressListener)
	
	// Enables or disables animations based on whether this component is added to the main stack hierarchy
	addStackHierarchyChangeListener { isAttached =>
		// May enable or disable animations
		if (isAttached)
		{
			progressPointer.removeListener(InvisibleProgressListener)
			actorHandler += ProgressDrawer
			progressPointer.addListener(TargetUpdateListener)
		}
		else
		{
			actorHandler -= ProgressDrawer
			progressPointer.removeListener(TargetUpdateListener)
			
			if (progressPointer.value >= 1)
				isCompletedFlag.set()
			else
				progressPointer.addListenerAndSimulateEvent(0.0)(InvisibleProgressListener)
		}
	}
	
	
	// COMPUTED	--------------------------
	
	/**
	  * @return Current progress of the tracked data
	  */
	def progress = progressPointer.value
	
	/**
	 * @param exc Implicit execution context
	 * @return A future of the event when this bar has been filled (also completes if the listened progress
	 *         completes while this component is not shown)
	 */
	def completionFuture(implicit exc: ExecutionContext) = isCompletedFlag.futureWhere { c => c }
		.map { _ => () }
	
	
	// IMPLEMENTED	----------------------
	
	override def component = label.component
	
	override protected def wrapped = label
	
	override def drawable = label
	
	
	// NESTED	--------------------------
	
	private object TargetUpdateListener extends ChangeListener[Double]
	{
		override def onChangeEvent(event: ChangeEvent[Double]) = ProgressDrawer.updateTargetProgress(event.newValue)
	}
	
	private object InvisibleProgressListener extends ChangeListener[Double]
	{
		override def onChangeEvent(event: ChangeEvent[Double]) =
		{
			if (event.newValue >= 1)
			{
				isCompletedFlag.set()
				progressPointer.removeListener(this)
			}
		}
	}
	
	private object ProgressDrawer extends CustomDrawer with Actor with Handleable
	{
		// ATTRIBUTES	------------------
		
		private var currentProgressAnimation: Animation[Double] = Animation.fixed(progress)
		private var currentAnimationProgress: Double = 1.0
		
		
		// COMPUTED	----------------------
		
		private def displayedProgress = currentProgressAnimation(currentAnimationProgress)
		
		
		// IMPLEMENTED	------------------
		
		override def opaque = false
		
		override def act(duration: FiniteDuration) =
		{
			// Progresses the progress animation
			if (currentAnimationProgress < 1.0)
			{
				val increase = duration / animationDuration
				currentAnimationProgress = (currentAnimationProgress + increase) min 1.0
				repaint()
				
				if (currentAnimationProgress >= 1 && displayedProgress >= 1)
					isCompletedFlag.set()
			}
		}
		
		override def drawLevel = Normal
		
		override def draw(drawer: Drawer, bounds: Bounds) =
		{
			// Determines the drawn progress
			val drawnProgress = displayedProgress
			
			// Draws background first
			val barBounds = if (bounds.width > bounds.height) bounds else
			{
				val height = bounds.width
				val translation = (bounds.height - height) / 2
				Bounds(bounds.position.plusY(translation), bounds.size.withHeight(height))
			}
			
			val rounded = barBounds.toRoundedRectangle(1)
			
			if (drawnProgress < 1)
				drawer.onlyFill(backgroundColor).draw(rounded)
			
			// Next draws the progress
			if (drawnProgress > 0)
			{
				if (drawnProgress == 1)
					drawer.onlyFill(barColor).draw(rounded)
				else
				{
					val partial = barBounds.mapSize { _ * (X -> drawnProgress) }
					drawer.clippedTo(rounded).onlyFill(barColor).draw(partial)
				}
			}
		}
		
		
		// OTHER	------------------------
		
		def updateTargetProgress(newTarget: Double) =
		{
			val startingValue = displayedProgress
			// Will not update past 0 or 100%
			val transition = ((newTarget min 1.0) max 0.0) - startingValue
			if (transition != 0)
			{
				currentProgressAnimation = Animation { p => startingValue + p * transition }.projectileCurved
				currentAnimationProgress = 0.0
			}
		}
	}
}
