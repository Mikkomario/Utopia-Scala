package utopia.reflection.component.swing.display

import utopia.flow.event.{ChangeEvent, ChangeListener, Changing}
import utopia.flow.util.TimeExtensions._
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
import utopia.reflection.shape.StackSize

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
	def contextual(stackSize: StackSize, progressPointer: Changing[Double])(implicit context: ColorContextLike,
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
				  progressPointer: Changing[Double], animationDuration: FiniteDuration = 0.25.seconds)
	extends StackableWrapper with CustomDrawableWrapper with SwingComponentRelated
{
	// ATTRIBUTES	---------------------
	
	private val label = StackSpace.drawingWith(ProgressDrawer, _stackSize)
	
	
	// COMPUTED	--------------------------
	
	/**
	  * @return Current progress of the tracked data
	  */
	def progress = progressPointer.value
	
	
	// IMPLEMENTED	----------------------
	
	override def isAttachedToMainHierarchy_=(newAttachmentStatus: Boolean) =
	{
		super.isAttachedToMainHierarchy_=(newAttachmentStatus)
		// May enable or disable animations
		if (newAttachmentStatus)
		{
			actorHandler += ProgressDrawer
			progressPointer.addListener(TargetUpdateListener)
		}
		else
		{
			actorHandler -= ProgressDrawer
			progressPointer.removeListener(TargetUpdateListener)
		}
	}
	
	override def component = label.component
	
	override protected def wrapped = label
	
	override def drawable = label
	
	
	// NESTED	--------------------------
	
	private object TargetUpdateListener extends ChangeListener[Double]
	{
		override def onChangeEvent(event: ChangeEvent[Double]) = ProgressDrawer.updateTargetProgress(event.newValue)
	}
	
	private object ProgressDrawer extends CustomDrawer with Actor with Handleable
	{
		// ATTRIBUTES	------------------
		
		private var currentProgressAnimation: Animation[Double] = Animation.fixed(progress)
		private var currentAnimationProgress: Double = 1.0
		
		
		// COMPUTED	----------------------
		
		private def displayedProgress = currentProgressAnimation(currentAnimationProgress)
		
		
		// IMPLEMENTED	------------------
		
		override def act(duration: FiniteDuration) =
		{
			// Progresses the progress animation
			if (currentAnimationProgress < 1.0)
			{
				val increase = duration / animationDuration
				currentAnimationProgress = (currentAnimationProgress + increase) min 1.0
				repaint()
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
					val partial = barBounds.mapSize { _ * (drawnProgress, X) }
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
