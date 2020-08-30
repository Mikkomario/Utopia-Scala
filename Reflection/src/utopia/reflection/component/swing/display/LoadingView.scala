package utopia.reflection.component.swing.display

import utopia.flow.event.{ChangeListener, Changing}
import utopia.genesis.shape.shape2D.Direction2D.Up
import utopia.reflection.component.context.{AnimationContextLike, TextContext}
import utopia.reflection.component.swing.label.TextLabel
import utopia.reflection.component.swing.template.StackableAwtComponentWrapperWrapper
import utopia.reflection.container.stack.StackLayout.Center
import utopia.reflection.container.swing.AwtContainerRelated
import utopia.reflection.container.swing.layout.multi.Stack
import utopia.reflection.container.swing.layout.multi.Stack.AwtStackable
import utopia.reflection.shape.Alignment.BottomLeft
import utopia.reflection.shape.LengthExtensions.LengthNumber
import utopia.reflection.shape.stack.modifier.{ExpandingLengthModifier, NoShrinkingConstraint}
import utopia.reflection.util.ProgressState

import scala.concurrent.ExecutionContext

object LoadingView
{
	/**
	  * Creates a new loading view
	  * @param loadingLabel A label that indicates this is a loading process (oftentimes an AnimatedLabel)
	  * @param progressPointer A pointer to the tracked progress
	  * @param defaultWidth Default width for the description label as well as the progress bar
	  * @param context Component creation context (implicit)
	  * @param animationContext Animation context (implicit)
	  * @return A new loading view
	  */
	def apply(loadingLabel: AwtStackable, progressPointer: Changing[ProgressState], defaultWidth: Double)
			 (implicit context: TextContext, animationContext: AnimationContextLike) =
		new LoadingView(loadingLabel, progressPointer, defaultWidth, context)
}

/**
  * Displays a progress bar with a loading icon and text that updates based on progress
  * @author Mikko Hilpinen
  * @since 30.8.2020, v1.2.1
  */
class LoadingView(loadingLabel: AwtStackable, progressPointer: Changing[ProgressState], defaultWidth: Double,
				  context: TextContext)(implicit animationContext: AnimationContextLike)
	extends StackableAwtComponentWrapperWrapper with AwtContainerRelated
{
	// ATTRIBUTES	---------------------------
	
	private val progressBar = context.use { implicit c =>
		ProgressBar.contextual(defaultWidth.any.expanding x c.margins.medium.downTo(c.margins.small),
			progressPointer.map { _.progress })
	}
	private val statusLabel = context.expandingToRight.withTextAlignment(BottomLeft).expandingTo(Up).use { implicit c =>
		val label = TextLabel.contextual(progressPointer.value.description)
		label.addWidthConstraint(new NoShrinkingConstraint(defaultWidth.any.expanding) && ExpandingLengthModifier)
		label
	}
	private lazy val progressListener: ChangeListener[ProgressState] = e => statusLabel.text = e.newValue.description
	
	// The view consists of an animated label on the left, followed by a description and a progress bar combination
	private val view =
	{
		context.use { implicit c =>
			Stack.buildRowWithContext(layout = Center) { s =>
				s += loadingLabel
				s += Stack.buildColumnWithContext(isRelated = true) { col =>
					col += statusLabel
					col += progressBar
				}
			}.framed(c.margins.medium.upscaling.square, c.containerBackground)
		}
	}
	
	
	// INITIAL CODE	------------------------
	
	// While attached to the main stack hierarchy, listens to changes in the progress pointer
	addStackHierarchyChangeListener { isAttached =>
		if (isAttached)
		{
			statusLabel.text = progressPointer.value.description
			progressPointer.addListener(progressListener)
		}
		else
			progressPointer.removeListener(progressListener)
	}
	
	
	// COMPUTED	----------------------------
	
	/**
	  * @param exc Implicit execution context
	  * @return A future that returns when the progress bar has completed its animation (also returns if followed
	  *         progress completes while the progress bar is not visible)
	  */
	def completionFuture(implicit exc: ExecutionContext) = progressBar.completionFuture
	
	
	// IMPLEMENTED	------------------------
	
	override def component = view.component
	
	override protected def wrapped = view
}
