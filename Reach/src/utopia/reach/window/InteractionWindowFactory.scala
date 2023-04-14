package utopia.reach.window

import utopia.firmament.component.Window
import utopia.firmament.context.{ColorContext, TextContext}
import utopia.firmament.model.stack.LengthExtensions._
import utopia.firmament.model.{HotKey, WindowButtonBlueprint}
import utopia.flow.async.AsyncExtensions.RichFuture
import utopia.flow.collection.CollectionExtensions._
import utopia.flow.view.immutable.View
import utopia.flow.view.immutable.eventful.AlwaysTrue
import utopia.flow.view.mutable.Pointer
import utopia.paradigm.color.ColorRole
import utopia.paradigm.enumeration.Alignment
import utopia.paradigm.enumeration.Axis.X
import utopia.reach.component.button.image.ImageAndTextButton
import utopia.reach.component.button.text.TextButton
import utopia.reach.component.factory.{ContextualMixed, Mixed}
import utopia.reach.component.template.{ButtonLike, ReachComponentLike}
import utopia.reach.container.ReachCanvas2
import utopia.reach.container.wrapper.{AlignFrame, Framing}
import utopia.reach.cursor.CursorSet
import utopia.firmament.model.enumeration.WindowResizePolicy.Program
import utopia.firmament.localization.LocalizedString
import utopia.firmament.model.stack.StackLength
import utopia.reach.container.multi.{ContextualStackFactory, Stack, StackFactory}

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
	  * @return Execution context used for performing asynchronous tasks
	  */
	protected def executionContext: ExecutionContext
	
	/**
	  * @return Context used when creating the dialog. Provided container background specifies the dialog background color.
	  */
	protected def standardContext: ColorContext
	
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
	protected def createContent(factories: ContextualMixed[ColorContext]): (ReachComponentLike, Vector[WindowButtonBlueprint[A]], View[Boolean])
	
	/**
	  * @return Result provided when no result is gained through interacting with the buttons
	  */
	protected def defaultResult: A
	
	/**
	  * @return Title displayed on this dialog
	  */
	protected def title: LocalizedString
	
	
	// OTHER	-----------------------
	
	/**
	  * Displays an interactive dialog to the user. Blocks while the dialog is visible
	  * @param parentWindow Window over which this window is displayed
	  */
	def displayBlockingOver(parentWindow: java.awt.Window, cursors: Option[CursorSet] = None) =
		displayBlocking(Some(parentWindow), cursors)
	
	/**
	  * Displays an interactive dialog to the user. Blocks while the dialog is visible
	  * @param parentWindow Window over which this window is displayed (optional)
	  */
	def displayBlocking(parentWindow: Option[java.awt.Window] = None, cursors: Option[CursorSet] = None): Try[A] =
		display(parentWindow, cursors)._2.waitFor()
	
	/**
	 * Displays an interactive dialog to the user
	 * @param parentWindow Window that will "own" the new window. None if the new window should be independent (default)
	 * @return 1: The window that was just opened, and
	  *        2: a future of the closing of the window, with a selected result (or default if none was selected)
	 */
	def displayOver(parentWindow: java.awt.Window, cursors: Option[CursorSet] = None) =
		display(Some(parentWindow), cursors)
	
	/**
	  * Displays an interactive window to the user
	  * @param parentWindow Window that will "own" the new window. None if the new window should be independent (default)
	  * @return 1: The window that was just opened, and
	  *         2: a future of the closing of the window, with a selected result (or default if none was selected)
	  */
	// FIXME: Rewrite this using ReachWindow
	def display(parentWindow: Option[java.awt.Window] = None,
	            cursors: Option[CursorSet] = None): (Window, Future[A]) =
	{
		/*
		implicit val exc: ExecutionContext = executionContext
		val context = standardContext
		
		val resultPromise = Promise[A]()
		// When this function finishes, this pointer contains the actually used condition
		// (the condition is built incrementally)
		val enterEnabledConditionPointer = Pointer[View[Boolean]](AlwaysTrue)
		
		// Creates the main content stack with 1-3 rows (based on button layouts)
		// TODO: Content should be allowed to appear outside (above) the framing, e.g. when displaying a header.
		//  Alternatively room should be allowed for a header component separately
		val (content, buttons) = ReachCanvas2(cursors) { hierarchy =>
			Framing(hierarchy).buildFilledWithContext(context, context.background, Stack)
				.apply(context.margins.medium.any) { stackF: ContextualStackFactory[ColorContext] =>
					stackF.build(Mixed).column() { factories =>
						// Creates the main content and determines the button blueprints
						val (mainContent, buttonBlueprints, defaultActionEnabledPointer) = createContent(factories)
						enterEnabledConditionPointer.value = defaultActionEnabledPointer
						
						// Groups the buttons based on location
						val (bottomButtons, topButtons) = buttonBlueprints.divideBy { _.location.isTop }
						
						// Places the main content and the buttons in a vertical stack
						val factoriesWithoutContext = factories.withoutContext
						val defaultButtonMargin = context.stackMargin.optimal
						val rowsBuilder = new VectorBuilder[ReachComponentLike]()
						val buttonsBuilder = new VectorBuilder[ButtonLike]()
						
						// Appends a single button row, if necessary
						def appendButtons(blueprints: Vector[WindowButtonBlueprint[A]]): Unit = {
							if (blueprints.nonEmpty) {
								val (component, buttons) = buttonRow(factoriesWithoutContext, blueprints,
									defaultButtonMargin, resultPromise, enterEnabledConditionPointer.value.value)
								rowsBuilder += component
								buttonsBuilder ++= buttons
							}
						}
						
						appendButtons(topButtons)
						// Adds the main content to the center
						rowsBuilder += mainContent
						appendButtons(bottomButtons)
						
						rowsBuilder.result() -> buttonsBuilder.result()
					}
				}
		}.parentAndResult
		
		// Creates and sets up the dialog
		val window = Window(content, parentWindow, title, Program, context.margins.medium, content.anchorPosition(_))
		
		// Displays the dialog
		window.startEventGenerators(context.actorHandler)
		window.display()
		
		// Finalizes the enter action enabled -function
		// Triggers the default button on enter, provided that no button has focus and the window does
		enterEnabledConditionPointer.update { baseCondition =>
			View(baseCondition.value && buttons.forall { !_.hasFocus } && window.isFocusedWindow && window.visible)
		}
		
		// Closes the dialog once a result is acquired.
		// Also completes the result with a default value if the dialog was closed without producing other result
		val resultFuture = resultPromise.future
		resultFuture.onComplete { _ => window.close() }
		window.closeFuture.onComplete { _ => if (!resultPromise.isCompleted) resultPromise.trySuccess(defaultResult) }
		
		window -> resultFuture
		 */
		???
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
					stackF.build(Mixed).apply(X, margin = nonScalingMargin) { factories =>
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
		factory.build(Mixed)(X, margin = scalingMargin) { factories =>
			val (components, buttons) = buttonGroups.splitMap { group =>
				// If there are multiple buttons in a group, places them in a stack
				if (group.size > 1) {
					factories(Stack).build(Mixed)(X, margin = nonScalingMargin) { factories =>
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
		implicit val context: TextContext = buttonContext(blueprint.role, blueprint.icon.isDefined)
		val enterHotkey = {
			if (blueprint.isDefault)
				Some(HotKey.conditionalKeyWithIndex(KeyEvent.VK_ENTER)(defaultActionEnabled))
			else
				None
		}
		val hotkeys = blueprint.hotkey.toSet ++ enterHotkey
		val button = blueprint.icon match {
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
