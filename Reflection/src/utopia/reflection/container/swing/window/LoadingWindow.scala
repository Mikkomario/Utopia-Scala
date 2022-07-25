package utopia.reflection.container.swing.window

import java.time.Instant
import utopia.flow.async.AsyncExtensions._
import utopia.flow.async.Delay
import utopia.flow.event.ChangingLike
import utopia.flow.time.TimeExtensions._
import utopia.flow.util.logging.Logger
import utopia.reflection.component.context.{AnimationContextLike, TextContext}
import utopia.reflection.component.swing.display.LoadingView
import utopia.reflection.container.swing.layout.multi.Stack.AwtStackable
import utopia.reflection.container.swing.window.WindowResizePolicy.Program
import utopia.reflection.localization.LocalizedString
import utopia.reflection.shape.Alignment
import utopia.reflection.util.ProgressState

import scala.concurrent.{ExecutionContext, Future}

/**
  * Used for displaying loading view on a separate window during a background process
  * @author Mikko Hilpinen
  * @since 30.8.2020, v1.3
  * @param loadingLabel A label that indicates a loading process (call by name, usually an AnimatedLabel)
  * @param progressPointer A pointer to the tracked progress
  * @param defaultWidth Default width for the progress bar
  * @param title Window title (default = empty string)
  * @param context Component creation context (implicit)
  * @param animationContext Component animation context (implicit)
  */
class LoadingWindow(loadingLabel: => AwtStackable, progressPointer: ChangingLike[ProgressState], defaultWidth: Double,
					title: LocalizedString = LocalizedString.empty)
				   (implicit context: TextContext, animationContext: AnimationContextLike)
{
	// ATTRIBUTES	--------------------------
	
	private lazy val content = LoadingView(loadingLabel, progressPointer, defaultWidth)
	
	
	// OTHER	------------------------------
	
	/**
	  * Displays this window during the loading process
	  * @param parentWindow Window that will host this window (optional)
	  * @param exc Implicit execution context
	  * @return A future that completes when the loading process has been completed
	  */
	def display(parentWindow: Option[java.awt.Window] = None)(implicit exc: ExecutionContext, logger: Logger) =
	{
		// Presents the window only if there is some loading still to be done
		if (progressPointer.value.progress < 1)
		{
			val loadingStarted = Instant.now()
			
			val window = parentWindow match
			{
				case Some(parent) => new Dialog(parent, content, title, Program, Alignment.Left)
				case None => Frame.windowed(content, title, Program, Alignment.Left)
			}
			
			// Delays the window display a little, in case the loading progress was very short
			Delay(loadingStarted + 0.25.seconds) {
				if (progressPointer.value.progress < 1) {
					// Displays the window
					window.startEventGenerators(context.actorHandler)
					window.display()
					
					// Closes the window once background processing has completed
					content.completionFuture.waitFor()
					window.close()
				}
				else
					window.close()
			}
		}
		else
			Future.successful(())
	}
	
	/**
	  * Displays this window during the loading process
	  * @param window Window that will host this window
	  * @param exc Implicit execution context
	  * @return A future that completes when the loading process has been completed
	  */
	def displayOver(window: java.awt.Window)(implicit exc: ExecutionContext, logger: Logger) =
		display(Some(window))
	
	/**
	  * Displays this window during the loading process
	  * @param window Window that will host this window
	  * @param exc Implicit execution context
	  * @return A future that completes when the loading process has been completed
	  */
	def displayOver(window: Window[_])(implicit exc: ExecutionContext, logger: Logger): Future[Unit] =
		displayOver(window.component)
}
