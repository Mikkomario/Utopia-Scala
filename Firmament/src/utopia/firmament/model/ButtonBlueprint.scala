package utopia.firmament.model

import utopia.firmament.context.ComponentCreationDefaults.componentLogger
import utopia.firmament.image.SingleColorIcon
import utopia.firmament.localization.LocalizedString
import utopia.flow.view.immutable.eventful.{AlwaysFalse, AlwaysTrue}
import utopia.flow.view.template.eventful.{Changing, FlagLike}
import utopia.paradigm.color.ColorRole
import utopia.paradigm.color.ColorRole.Primary
import utopia.paradigm.enumeration.Alignment
import utopia.paradigm.enumeration.Alignment.BottomRight

import scala.concurrent.{ExecutionContext, Future, Promise}
import scala.util.{Failure, Success}

object ButtonBluePrint
{
	// OTHER    ------------------------
	
	/**
	 * @param text           Text to display in this button (default = empty)
	 * @param icon           Icon to display in this button (default = empty)
	 * @param role           Color role used by this button (default = primary).
	 * @param location       The location where the button should be placed (default = bottom right)
	 * @param hotKey         Hotkey that is used for triggering this button, even when it is not in focus (optional)
	 * @param visiblePointer A pointer that contains true while this button should be displayed (default = always true)
	 * @param enabledPointer A pointer that contains true while this button is interactive (default = always true)
	 * @param isDefault      Whether this button should be treated as the default form button (default = false)
	 */
	def apply(text: LocalizedString = LocalizedString.empty,
	          icon: SingleColorIcon = SingleColorIcon.empty,
	          role: ColorRole = Primary, location: Alignment = BottomRight,
	          hotKey: Option[HotKey] = None, visiblePointer: FlagLike = AlwaysTrue,
	          enabledPointer: FlagLike = AlwaysTrue, isDefault: Boolean = false) =
		ButtonBlueprintFactory(text, icon, role, location, hotKey, visiblePointer, enabledPointer, isDefault)
	
	
	// NESTED   ------------------------
	
	/**
	 * @param text           Text to display in this button (default = empty)
	 * @param icon           Icon to display in this button (default = empty)
	 * @param role           Color role used by this button (default = primary).
	 * @param location       The location where the button should be placed (default = bottom right)
	 * @param hotKey         Hotkey that is used for triggering this button, even when it is not in focus (optional)
	 * @param visiblePointer A pointer that contains true while this button should be displayed (default = always true)
	 * @param enabledPointer A pointer that contains true while this button is interactive (default = always true)
	 * @param isDefault      Whether this button should be treated as the default form button (default = false)
	 */
	case class ButtonBlueprintFactory(text: LocalizedString = LocalizedString.empty,
	                                  icon: SingleColorIcon = SingleColorIcon.empty,
	                                  role: ColorRole = Primary, location: Alignment = BottomRight,
	                                  hotKey: Option[HotKey] = None, visiblePointer: FlagLike = AlwaysTrue,
	                                  enabledPointer: FlagLike = AlwaysTrue, isDefault: Boolean = false)
		extends ButtonBluePrintTemplate
	{
		// COMPUTED -------------------
		
		/**
		 * @return Access to methods that yield buttons which perform their actions asynchronously,
		 *         using loading visuals when available.
		 */
		def loading = new AsyncButtonBluePrintFactory(useLoadingState = true)
		/**
		 * @return Access to methods that yield buttons which perform their actions asynchronously,
		 *         and are disabled while doing so.
		 */
		def disabledDuring = new AsyncButtonBluePrintFactory(useLoadingState = false)
		
		
		// OTHER    -------------------
		
		/**
		 * Creates a button that resolves immediately, when pressed
		 * @param action The action that occurs when this button is pressed (blocking).
		 *               Returns the form result to yield.
		 * @tparam A Type of form result returned.
		 * @return A new button blueprint
		 */
		def immediate[A](action: => A) = apply[A](loadingEnabled = false) { promise =>
			promise.trySuccess(action)
			AlwaysFalse
		}
		/**
		 * Creates a button that resolves immediately when pressed,
		 * but which doesn't necessarily complete the form it is in.
		 * @param action The action that occurs when this button is pressed (blocking).
		 *               Returns either:
		 *                  Some) The form result to yield, or
		 *                  None) In case the form shouldn't be completed
		 * @tparam A Type of form result returned.
		 * @return A new button blueprint
		 */
		def immediateAttempt[A](action: => Option[A]) = apply[A](loadingEnabled = false) { promise =>
			action.foreach(promise.trySuccess)
			AlwaysFalse
		}
		
		private def apply[A](loadingEnabled: Boolean)(act: Promise[A] => FlagLike): ButtonBluePrint[A] =
			new _BluePrint[A](act, loadingEnabled)
		
		
		// NESTED   -------------------
		
		class AsyncButtonBluePrintFactory(useLoadingState: Boolean)
		{
			/**
			 * Creates a button that resolves asynchronously.
			 * This button is not interactive while the asynchronous process is ongoing.
			 * @param action The action that occurs when this button is pressed.
			 *               Returns a future that resolves into the form result to yield.
			 * @tparam A Type of form result returned.
			 * @return A new button blueprint
			 */
			def apply[A](action: => Future[A])(implicit exc: ExecutionContext) =
				ButtonBlueprintFactory.this.apply[A](useLoadingState) { promise =>
					val f = action
					val pointer = Changing.completionOf(f)
					promise.completeWith(f)
					pointer
				}
			/**
			 * Creates a button that resolves asynchronously.
			 * This button is not interactive while the asynchronous process is ongoing.
			 * Doesn't necessarily complete the form.
			 * @param action The action that occurs when this button is pressed.
			 *               Returns a future that resolves into the form result to yield,
			 *               or to None, in case the form shouldn't be completed yet.
			 * @tparam A Type of form result returned.
			 * @return A new button blueprint
			 */
			def attempt[A](action: => Future[Option[A]])(implicit exc: ExecutionContext) =
				ButtonBlueprintFactory.this.apply[A](useLoadingState) { promise =>
					val f = action
					val pointer = Changing.completionOf(f)
					f.onComplete {
						case Success(res) => res.foreach(promise.trySuccess)
						case Failure(error) => promise.tryFailure(error)
					}
					pointer
				}
		}
		
		private class _BluePrint[A](act: Promise[A] => FlagLike, override val loadingEnabled: Boolean)
			extends ButtonBluePrint[A]
		{
			override protected def template: ButtonBluePrintTemplate = ButtonBlueprintFactory.this
			
			override def activate(resultPromise: Promise[A]): FlagLike = act(resultPromise)
		}
	}
}

trait ButtonBluePrintTemplate
{
	/**
	 * @return Text to display on this button
	 */
	def text: LocalizedString
	/**
	 * @return Icon to display on this button
	 */
	def icon: SingleColorIcon
	/**
	 * @return (Color) role of this button
	 */
	def role: ColorRole
	/**
	 * @return The preferred location of this button
	 */
	def location: Alignment
	/**
	 * @return The hotkey that activates this button, if applicable
	 */
	def hotKey: Option[HotKey]
	/**
	 * @return A pointer that contains true while this button should be displayed (default = always true)
	 */
	def visiblePointer: FlagLike
	/**
	 * @return A pointer that contains true while this button is interactive (default = always true)
	 */
	def enabledPointer: FlagLike
	/**
	 * @return Whether this is the default form button
	 */
	def isDefault: Boolean
}

trait ButtonBluePrint[A] extends ButtonBluePrintTemplate
{
	// ABSTRACT -----------------------
	
	/**
	 * @return The template that contains basic information about this button
	 */
	protected def template: ButtonBluePrintTemplate
	
	/**
	 * @return Whether the "loading" state should be used for this button, if available
	 */
	def loadingEnabled: Boolean
	
	/**
	 * @param resultPromise Promise that accepts the action result, if acquired
	 * @return A flag that contains true while this button shall be displayed as "loading" or disabled
	 */
	def activate(resultPromise: Promise[A]): FlagLike
	
	
	// IMPLEMENTED  ------------------
	
	override def text: LocalizedString = template.text
	override def icon: SingleColorIcon = template.icon
	override def role: ColorRole = template.role
	override def location: Alignment = template.location
	override def hotKey: Option[HotKey] = template.hotKey
	override def visiblePointer: FlagLike = template.visiblePointer
	override def enabledPointer: FlagLike = template.enabledPointer
	override def isDefault: Boolean = template.isDefault
}