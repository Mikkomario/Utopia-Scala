package utopia.reflection.component.swing.display

import utopia.firmament.context.color.StaticColorContext
import utopia.firmament.context.{AnimationContext, ComponentCreationDefaults}
import utopia.firmament.drawing.mutable.MutableCustomDrawableWrapper
import utopia.firmament.drawing.template.CustomDrawer
import utopia.firmament.model.stack.StackSize
import utopia.flow.event.listener.ChangeListener
import utopia.flow.event.model.ChangeEvent
import utopia.flow.event.model.ChangeResponse.{Continue, Detach}
import utopia.flow.view.mutable.async.{Volatile, VolatileFlag}
import utopia.flow.view.template.eventful.{Changing, Flag}
import utopia.genesis.graphics.DrawLevel.Normal
import utopia.genesis.graphics.{DrawSettings, Drawer}
import utopia.genesis.handling.action.{Actor, ActorHandler}
import utopia.paradigm.animation.Animation
import utopia.paradigm.animation.AnimationLike.AnyAnimation
import utopia.paradigm.color.Color
import utopia.paradigm.enumeration.Axis.{X, Y}
import utopia.paradigm.shape.shape2d.area.polygon.c4.bounds.Bounds
import utopia.reflection.component.swing.StackSpace
import utopia.reflection.component.swing.template.SwingComponentRelated
import utopia.reflection.component.template.layout.stack.ReflectionStackableWrapper

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
	def contextual(stackSize: StackSize, progressPointer: Changing[Double])(implicit context: StaticColorContext,
	                                                                        animationContext: AnimationContext) =
		new ProgressBar(animationContext.actorHandler, stackSize, context.color.gray, context.color.secondary,
			progressPointer, animationContext.animationDuration)
}

/**
  * Used for displaying progress of some longer operation
  * @author Mikko Hilpinen
  * @since 1.8.2019, v1+
  */
class ProgressBar(actorHandler: ActorHandler, _stackSize: StackSize, val backgroundColor: Color, val barColor: Color,
                  progressPointer: Changing[Double],
                  animationDuration: FiniteDuration = ComponentCreationDefaults.transitionDuration)
	extends ReflectionStackableWrapper with MutableCustomDrawableWrapper with SwingComponentRelated
{
	// ATTRIBUTES	---------------------
	
	private val bgDs = DrawSettings.onlyFill(backgroundColor)
	private val barDs = DrawSettings.onlyFill(barColor)
	
	private val label = StackSpace.drawingWith(ProgressDrawer, _stackSize)
	
	// Used for holding visible completion status, can be listened
	private val isCompletedFlag = VolatileFlag(progress >= 1)
	
	
	// INITIAL CODE ----------------------
	
	progressPointer.addListenerAndSimulateEvent(0.0)(InvisibleProgressListener)
	
	// Enables or disables animations based on whether this component is added to the main stack hierarchy
	addStackHierarchyChangeListener { isAttached =>
		// May enable or disable animations
		if (isAttached) {
			progressPointer.removeListener(InvisibleProgressListener)
			actorHandler += ProgressDrawer
			progressPointer.addListener(TargetUpdateListener)
		}
		else {
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
		override def onChangeEvent(event: ChangeEvent[Double]) =
			ProgressDrawer.updateTargetProgress(event.newValue)
	}
	
	private object InvisibleProgressListener extends ChangeListener[Double]
	{
		override def onChangeEvent(event: ChangeEvent[Double]) = {
			if (event.newValue >= 1) {
				isCompletedFlag.set()
				Detach
			}
			else
				Continue
		}
	}
	
	private object ProgressDrawer extends CustomDrawer with Actor
	{
		// ATTRIBUTES	------------------
		
		private val animationProgressPointer = Volatile.eventful(1.0)
		override val handleCondition: Flag = animationProgressPointer.map { _ <= 1.0 }
		
		private var currentProgressAnimation: AnyAnimation[Double] = Animation.fixed(progress)
		
		
		// COMPUTED	----------------------
		
		private def displayedProgress = currentProgressAnimation(animationProgressPointer.value)
		
		
		// IMPLEMENTED	------------------
		
		override def opaque = false
		
		override def act(duration: FiniteDuration) = {
			// Progresses the progress animation
			val increase = duration / animationDuration
			val newProgress = animationProgressPointer.updateAndGet { p => (p + increase) min 1.0 }
			repaint()
			
			if (newProgress >= 1 && displayedProgress >= 1)
				isCompletedFlag.set()
		}
		
		override def drawLevel = Normal
		
		override def draw(drawer: Drawer, bounds: Bounds) = {
			// Determines the drawn progress
			val drawnProgress = displayedProgress
			
			// Draws background first
			val barBounds = if (bounds.width > bounds.height) bounds else {
				val height = bounds.width
				val translation = (bounds.height - height) / 2
				Bounds(bounds.position + Y(translation), bounds.size.withHeight(height))
			}
			
			val rounded = barBounds.toRoundedRectangle(1)
			
			if (drawnProgress < 1)
				drawer.draw(rounded)(bgDs)
			
			// Next draws the progress
			if (drawnProgress > 0) {
				if (drawnProgress == 1)
					drawer.draw(rounded)(barDs)
				else {
					val partial = barBounds.mapSize { _.scaledAlong(X(drawnProgress)) }
					drawer.withMutatedGraphics { _.clip(rounded) }.draw(partial)(barDs)
				}
			}
		}
		
		
		// OTHER	------------------------
		
		def updateTargetProgress(newTarget: Double) = {
			val startingValue = displayedProgress
			// Will not update past 0 or 100%
			val transition = ((newTarget min 1.0) max 0.0) - startingValue
			if (transition != 0) {
				currentProgressAnimation = Animation { p => startingValue + p * transition }.projectileCurved
				animationProgressPointer.value = 0.0
			}
		}
	}
}
