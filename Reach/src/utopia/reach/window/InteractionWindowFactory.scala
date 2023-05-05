package utopia.reach.window

import utopia.firmament.component.Window
import utopia.firmament.context.TextContext
import utopia.firmament.localization.LocalizedString
import utopia.firmament.model.stack.LengthExtensions._
import utopia.firmament.model.stack.StackLength
import utopia.firmament.model.{HotKey, WindowButtonBlueprint}
import utopia.flow.async.AsyncExtensions.RichFuture
import utopia.flow.collection.CollectionExtensions._
import utopia.flow.util.logging.Logger
import utopia.flow.view.immutable.View
import utopia.flow.view.mutable.eventful.SettableOnce
import utopia.paradigm.color.ColorRole
import utopia.paradigm.enumeration.Alignment
import utopia.paradigm.enumeration.Axis.X
import utopia.reach.component.button.image.ImageAndTextButton
import utopia.reach.component.button.text.TextButton
import utopia.reach.component.factory.{ContextualMixed, Mixed}
import utopia.reach.component.template.{ButtonLike, ReachComponentLike}
import utopia.reach.component.wrapper.ComponentCreationResult
import utopia.reach.container.multi.{Stack, StackFactory}
import utopia.reach.container.wrapper.{AlignFrame, Framing}
import utopia.reach.context.ReachContentWindowContext

import java.awt.event.KeyEvent
import scala.collection.immutable.VectorBuilder
import scala.concurrent.{ExecutionContext, Future, Promise}
import scala.util.Try

/**
  * A common trait for creating dialogs that are used for user-interactions, usually for requesting some sort of input
  * @author Mikko Hilpinen
  * @since 1.3.2021, v0.1
  * @tparam A Type of results yielded by the generated windows
  */
// TODO: Should allow for changing buttons
trait InteractionWindowFactory[A]
{
	// ABSTRACT	-----------------------
	
	/**
	  * @return Context used when creating windows.
	 *         Defines the default settings to use for the content as well.
	  */
	protected def windowContext: ReachContentWindowContext
	/**
	  * @return Execution context used for performing asynchronous tasks
	  */
	protected def executionContext: ExecutionContext
	/**
	  * @return A logging implementation for encountered errors
	  */
	protected def log: Logger
	
	/**
	  * @return Title displayed on this dialog
	  */
	protected def title: LocalizedString
	
	/**
	  * @return Result provided when no result is gained through interacting with the buttons
	  */
	protected def defaultResult: A
	
	/**
	  * @param buttonColor The desired button color
	  * @param hasIcon Whether the button uses an icon
	  * @return Context used when creating the button
	  */
	protected def buttonContext(buttonColor: ColorRole, hasIcon: Boolean): TextContext
	
	/**
	  * Creates new content for a new dialog
	  * @param factories Factories used for creating the main content
	  * @return The main content + list of button blueprints + pointer to whether the default button may be
	  *         triggered by pressing enter inside this window
	  */
	protected def createContent(factories: ContextualMixed[TextContext]): (ReachComponentLike, Vector[WindowButtonBlueprint[A]], View[Boolean])
	
	
	// OTHER	-----------------------
	
	/**
	  * Displays an interactive dialog to the user. Blocks while the dialog is visible
	  * @param parentWindow Window over which this window is displayed
	  * @return The selected result (or the default result)
	  */
	def displayBlockingOver(parentWindow: java.awt.Window) = displayBlocking(Some(parentWindow))
	/**
	  * Displays an interactive dialog to the user. Blocks while the dialog is visible
	  * @param parentWindow Window over which this window is displayed (optional)
	  * @return The selected result (or the default result)
	  */
	def displayBlocking(parentWindow: Option[java.awt.Window] = None): Try[A] = display(parentWindow).result.waitFor()
	/**
	 * Displays an interactive dialog to the user
	 * @param parentWindow Window that will "own" the new window. None if the new window should be independent (default)
	 * @return 1: The window that was just opened as the main result, and
	  *        2: a future of the closing of the window, with a selected result (or default if none was selected),
	  *        as an additional result
	 */
	def displayOver(parentWindow: java.awt.Window) =
		display(Some(parentWindow))
	/**
	  * Displays an interactive window to the user
	  * @param parentWindow Window that will "own" the new window.
	  *                     None if the new window should be independent (default)
	  * @return 1: The window that was just opened as the main result, and
	  *         2: a future of the closing of the window, with a selected result (or default if none was selected),
	  *         as an additional result
	  */
	def display(parentWindow: Option[java.awt.Window] = None): ComponentCreationResult[Window, Future[A]] =
	{
		implicit val wc: ReachContentWindowContext = windowContext
		implicit val exc: ExecutionContext = executionContext
		implicit val log: Logger = this.log
		
		val resultPromise = Promise[A]()
		// When this function finishes, this pointer contains the actually used condition
		// (the condition is built incrementally)
		val enterEnabledPointerPointer = SettableOnce[View[Boolean]]()
		
		// Creates the window and the main content stack with 1-3 rows (based on button layouts)
		// TODO: Content should be allowed to appear outside (above) the framing, e.g. when displaying a header.
		//  Alternatively room should be allowed for a header component separately
		// Contains the created buttons and the default action enabled -pointer as the additional result
		val window = ReachWindow.contentContextual.using(Framing, parentWindow, title) { (_, framingF) =>
			framingF.build(Stack).apply(wc.margins.aroundMedium) { stackF =>
				stackF.build(Mixed) { factories =>
					// Creates the main content and determines the button blueprints
					val (mainContent, buttonBlueprints, defaultActionEnabledPointer) = createContent(factories)
					
					// Groups the buttons based on location
					val (bottomButtons, topButtons) = buttonBlueprints.divideBy { _.location.isTop }
					
					// Places the main content and the buttons in a vertical stack
					val factoriesWithoutContext = factories.withoutContext
					val defaultButtonMargin = factories.context.stackMargin.optimal
					val rowsBuilder = new VectorBuilder[ReachComponentLike]()
					val buttonsBuilder = new VectorBuilder[ButtonLike]()
					
					// Appends a single button row, if necessary
					def appendButtons(blueprints: Vector[WindowButtonBlueprint[A]]): Unit = {
						if (blueprints.nonEmpty) {
							val (component, buttons) = buttonRow(factoriesWithoutContext, blueprints,
								defaultButtonMargin, resultPromise,
								enterEnabledPointerPointer.value.exists { _.value })
							rowsBuilder += component
							buttonsBuilder ++= buttons
						}
					}
					
					appendButtons(topButtons)
					// Adds the main content to the center
					rowsBuilder += mainContent
					appendButtons(bottomButtons)
					
					rowsBuilder.result() -> (buttonsBuilder.result(), defaultActionEnabledPointer)
				}
			}
		}
		
		// Displays the dialog
		window.display(centerOnParent = true)
		
		// Finalizes the enter action enabled -function
		// Triggers the default button on enter, provided that no button has focus and the window does
		enterEnabledPointerPointer.set(View { window.isFullyVisible && window.isFocused && window.result._2.value })
		
		// Closes the dialog once a result is acquired.
		// Also completes the result with a default value if the dialog was closed without producing other result
		val resultFuture = resultPromise.future
		resultFuture.onComplete { _ => window.close() }
		window.closeFuture.onComplete { _ => if (!resultPromise.isCompleted) resultPromise.trySuccess(defaultResult) }
		
		ComponentCreationResult(window, resultFuture)
	}
	
	private def buttonRow(factories: Mixed, buttons: Vector[WindowButtonBlueprint[A]],
						  baseMargin: Double, resultPromise: Promise[A],
						  defaultActionEnabled: => Boolean): (ReachComponentLike, Vector[ButtonLike]) =
	{
		val nonScalingMargin = baseMargin.downscaling
		
		val buttonsByLocation = buttons.groupBy { _.location.onlyHorizontal }
		// Case: Items only on one side
		if (buttonsByLocation.size == 1)
		{
			val (alignment, blueprints) = buttonsByLocation.head
			// Case: More than one button
			if (blueprints.size > 1)
				factories(AlignFrame).build(Stack)(alignment) { stackF =>
					stackF.copy(axis = X, margin = nonScalingMargin).build(Mixed) { factories =>
						val buttons = blueprints.map { blueprint =>
							actualize(factories, blueprint, resultPromise, defaultActionEnabled)
						}
						buttons -> buttons
					}.parentAndResult
				}.parentAndResult
			// Case: Only one button
			else
				factories(AlignFrame).build(Mixed)(alignment) { factories =>
					val button = actualize(factories, blueprints.head, resultPromise, defaultActionEnabled)
					button -> Vector(button)
				}.parentAndResult
		}
		else
		{
			val scalingMargin = baseMargin.upscaling.expanding
			val buttonGroups = Vector(Alignment.Left, Alignment.Center, Alignment.Right).flatMap(buttonsByLocation.get)
			
			// Case: Items on left side + right and/or center => no aligning needed
			if (buttonsByLocation.contains(Alignment.Left))
				buttonGroupsToStack(factories(Stack), buttonGroups, scalingMargin, nonScalingMargin, resultPromise,
					defaultActionEnabled)
			// Case: Items only on center and right => aligns the stack
			else
				factories(AlignFrame).build(Stack)(Alignment.Right) { stackF =>
					buttonGroupsToStack(stackF, buttonGroups, scalingMargin, nonScalingMargin, resultPromise,
						defaultActionEnabled)
				}.parentAndResult
		}
	}
	
	private def buttonGroupsToStack(factory: StackFactory, buttonGroups: Vector[Vector[WindowButtonBlueprint[A]]],
									scalingMargin: StackLength, nonScalingMargin: StackLength,
									resultPromise: Promise[A],
									defaultActionEnabled: => Boolean): (Stack[ReachComponentLike], Vector[ButtonLike]) =
	{
		factory.copy(axis = X, margin = scalingMargin).build(Mixed) { factories =>
			val (components, buttons) = buttonGroups.splitMap { group =>
				// If there are multiple buttons in a group, places them in a stack
				if (group.size > 1) {
					factories(Stack).copy(axis = X, margin = nonScalingMargin).build(Mixed) { factories =>
						val buttons = group.map { blueprint =>
							actualize(factories, blueprint, resultPromise, defaultActionEnabled)
						}
						buttons -> buttons
					}.parentAndResult
				}
				else {
					val button = actualize(factories(Mixed), group.head, resultPromise, defaultActionEnabled)
					button -> Vector(button)
				}
			}
			components -> buttons.flatten
		}.parentAndResult
	}
	
	// Returns created button
	private def actualize(factories: Mixed, blueprint: WindowButtonBlueprint[A], resultPromise: Promise[A],
	                      defaultActionEnabled: => Boolean) =
	{
		implicit val context: TextContext = buttonContext(blueprint.role, blueprint.icon.nonEmpty)
		val enterHotkey = {
			if (blueprint.isDefault)
				Some(HotKey.conditionalKeyWithIndex(KeyEvent.VK_ENTER)(defaultActionEnabled))
			else
				None
		}
		val hotkeys = blueprint.hotkey.toSet ++ enterHotkey
		val button = blueprint.icon.notEmpty match {
			case Some(icon) =>
				factories(ImageAndTextButton).withContext(context).withIcon(icon, blueprint.text, hotKeys = hotkeys) {
					blueprint.pressAction(resultPromise)
				}
			case None =>
				factories(TextButton).withContext(context).apply(blueprint.text, hotkeys) {
					blueprint.pressAction(resultPromise)
				}
		}
		button
	}
}
