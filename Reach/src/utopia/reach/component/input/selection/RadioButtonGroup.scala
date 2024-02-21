package utopia.reach.component.input.selection

import utopia.firmament.component.display.Pool
import utopia.firmament.component.input.InteractionWithPointer
import utopia.firmament.context.TextContext
import utopia.firmament.drawing.template.CustomDrawer
import utopia.firmament.localization.LocalizedString
import utopia.firmament.model.HotKey
import utopia.firmament.model.enumeration.StackLayout
import utopia.firmament.model.stack.StackLength
import utopia.flow.view.immutable.eventful.AlwaysFalse
import utopia.flow.view.mutable.eventful.EventfulPointer
import utopia.flow.view.template.eventful.{Changing, FlagLike}
import utopia.genesis.handling.event.keyboard.{KeyStateEvent, KeyStateListener, KeyboardEvents}
import utopia.paradigm.color.ColorRole
import utopia.paradigm.enumeration.Axis.Y
import utopia.paradigm.enumeration.Axis2D
import utopia.reach.component.factory.FromVariableContextComponentFactoryFactory
import utopia.reach.component.factory.contextual.{VariableBackgroundRoleAssignableFactory, VariableContextualFactory}
import utopia.reach.component.hierarchy.ComponentHierarchy
import utopia.reach.component.input.check.RadioButtonLine
import utopia.reach.component.template.ReachComponentWrapper
import utopia.reach.container.multi.{Stack, StackSettings, StackSettingsLike}
import utopia.reach.focus.ManyFocusableWrapper

import scala.language.implicitConversions

/**
  * Common trait for radio button group factories and settings
  * @tparam Repr Implementing factory/settings type
  * @author Mikko Hilpinen
  * @since 21.06.2023, v1.1
  */
trait RadioButtonGroupSettingsLike[+Repr] extends StackSettingsLike[Repr]
{
	// ABSTRACT	--------------------
	
	def stackSettings: StackSettings
	/**
	  * Color role used for highlighting the selected radio button
	  */
	def selectedColorRole: ColorRole
	
	/**
	  * Color role used for highlighting the selected radio button
	  * @param role New selected color role to use.
	  *             Color role used for highlighting the selected radio button
	  * @return Copy of this factory with the specified selected color role
	  */
	def withSelectedColorRole(role: ColorRole): Repr
	/**
	  * @param settings New stack settings to use.
	  * @return Copy of this factory with the specified stack settings
	  */
	def withStackSettings(settings: StackSettings): Repr
	
	
	// IMPLEMENTED	--------------------
	
	override def axis = stackSettings.axis
	override def cap = stackSettings.cap
	override def customDrawers = stackSettings.customDrawers
	override def layout = stackSettings.layout
	
	override def withAxis(axis: Axis2D) = withStackSettings(stackSettings.withAxis(axis))
	override def withCap(cap: StackLength) = withStackSettings(stackSettings.withCap(cap))
	override def withCustomDrawers(drawers: Vector[CustomDrawer]) =
		withStackSettings(stackSettings.withCustomDrawers(drawers))
	override def withLayout(layout: StackLayout) = withStackSettings(stackSettings.withLayout(layout))
	
	
	// OTHER	--------------------
	
	def mapStackSettings(f: StackSettings => StackSettings) = withStackSettings(f(stackSettings))
}

object RadioButtonGroupSettings
{
	// ATTRIBUTES	--------------------
	
	val default = apply()
	
	
	// IMPLICIT ------------------------
	
	implicit def wrap(stackSettings: StackSettings): RadioButtonGroupSettings = apply(stackSettings)
}
/**
  * Combined settings used when constructing radio button groups
  * @param selectedColorRole Color role used for highlighting the selected radio button
  * @author Mikko Hilpinen
  * @since 21.06.2023, v1.1
  */
case class RadioButtonGroupSettings(stackSettings: StackSettings = StackSettings.default,
                                    selectedColorRole: ColorRole = ColorRole.Secondary)
	extends RadioButtonGroupSettingsLike[RadioButtonGroupSettings]
{
	// IMPLEMENTED	--------------------
	
	override def withSelectedColorRole(role: ColorRole) = copy(selectedColorRole = role)
	override def withStackSettings(settings: StackSettings) = copy(stackSettings = settings)
}

/**
  * Common trait for factories that wrap a radio button group settings instance
  * @tparam Repr Implementing factory/settings type
  * @author Mikko Hilpinen
  * @since 21.06.2023, v1.1
  */
trait RadioButtonGroupSettingsWrapper[+Repr] extends RadioButtonGroupSettingsLike[Repr]
{
	// ABSTRACT	--------------------
	
	/**
	  * Settings wrapped by this instance
	  */
	protected def settings: RadioButtonGroupSettings
	/**
	  * @return Copy of this factory with the specified settings
	  */
	def withSettings(settings: RadioButtonGroupSettings): Repr
	
	
	// IMPLEMENTED	--------------------
	
	override def selectedColorRole = settings.selectedColorRole
	override def stackSettings = settings.stackSettings
	override def withSelectedColorRole(role: ColorRole) = mapSettings { _.withSelectedColorRole(role) }
	override def withStackSettings(settings: StackSettings) = mapSettings { _.withStackSettings(settings) }
	
	
	// OTHER	--------------------
	
	def mapSettings(f: RadioButtonGroupSettings => RadioButtonGroupSettings) = withSettings(f(settings))
}

/**
  * Factory class used for constructing radio button groups using contextual component creation information
  * @author Mikko Hilpinen
  * @since 21.06.2023, v1.1
  */
case class ContextualRadioButtonGroupFactory(parentHierarchy: ComponentHierarchy,
                                             contextPointer: Changing[TextContext],
                                             settings: RadioButtonGroupSettings = RadioButtonGroupSettings.default,
                                             drawsBackground: Boolean = false)
	extends RadioButtonGroupSettingsWrapper[ContextualRadioButtonGroupFactory]
		with VariableContextualFactory[TextContext, ContextualRadioButtonGroupFactory]
		with VariableBackgroundRoleAssignableFactory[TextContext, ContextualRadioButtonGroupFactory]
{
	// IMPLEMENTED	------------------------
	
	override def withContextPointer(contextPointer: Changing[TextContext]) =
		copy(contextPointer = contextPointer)
	override def withSettings(settings: RadioButtonGroupSettings) =
		copy(settings = settings)
	
	override protected def withVariableBackgroundContext(newContextPointer: Changing[TextContext],
	                                                     backgroundDrawer: CustomDrawer): ContextualRadioButtonGroupFactory =
		copy(contextPointer = newContextPointer, settings = settings.withCustomBackgroundDrawer(backgroundDrawer),
			drawsBackground = true)
	
	
	// OTHER	----------------------------
	
	/**
	  * Creates a new radio button group
	  * @param options Selectable options
	  * @tparam A Type of selected value
	  * @return A new radio button group
	  * @throws IllegalArgumentException If the specified set of options is empty
	  */
	@throws[IllegalArgumentException]("If the specified set of options is empty")
	def apply[A](options: Vector[(A, LocalizedString)]): RadioButtonGroup[A] = {
		if (options.isEmpty)
			throw new IllegalArgumentException("There must be at least one available option")
		else
			apply(options, new EventfulPointer[A](options.head._1))
	}
	
	/**
	  * Creates a new radio button group
	  * @param options Selectable options
	  * @param valuePointer A mutable pointer to the currently selected option / value
	  * @param hotKeys Hotkeys used for triggering different options
	  * @tparam A Type of selected value
	  * @return A new radio button group
	  */
	def apply[A](options: Vector[(A, LocalizedString)], valuePointer: EventfulPointer[A],
	             hotKeys: Map[A, Set[HotKey]] = Map()) =
	{
		val group = new RadioButtonGroup[A](parentHierarchy, contextPointer, options, valuePointer, settings, hotKeys)
		if (drawsBackground)
			contextPointer.addContinuousListener { e =>
				if (e.values.isAsymmetricBy { _.background })
					group.repaint()
			}
		group
	}
	
	/**
	  * Creates a new radio button group
	  * @param options      Selectable options
	  * @param valuePointer A mutable pointer to the currently selected option / value
	  * @tparam A Type of selected value
	  * @return A new radio button group
	  */
	def apply[A](options: Vector[(A, LocalizedString)], valuePointer: EventfulPointer[A]): RadioButtonGroup[A] =
		apply[A](options, valuePointer, Map[A, Set[HotKey]]())
	@deprecated("Renamed to .apply(...)", "v1.1")
	def withPointer[A](options: Vector[(A, LocalizedString)], valuePointer: EventfulPointer[A]): RadioButtonGroup[A] =
		apply[A](options, valuePointer)
}

/**
  * Used for defining radio button group creation settings outside of the component building process
  * @author Mikko Hilpinen
  * @since 21.06.2023, v1.1
  */
case class RadioButtonGroupSetup(settings: RadioButtonGroupSettings = RadioButtonGroupSettings.default)
	extends RadioButtonGroupSettingsWrapper[RadioButtonGroupSetup]
		with FromVariableContextComponentFactoryFactory[TextContext, ContextualRadioButtonGroupFactory]
{
	// IMPLEMENTED	--------------------
	
	override def withContextPointer(hierarchy: ComponentHierarchy, p: Changing[TextContext]) =
		ContextualRadioButtonGroupFactory(hierarchy, p, settings)
	
	override def withSettings(settings: RadioButtonGroupSettings) = copy(settings = settings)
}

object RadioButtonGroup extends RadioButtonGroupSetup()
{
	// OTHER	--------------------
	
	def apply(settings: RadioButtonGroupSettings) = withSettings(settings)
}

/**
  * A user input based on a multitude of radio buttons
  * @author Mikko Hilpinen
  * @since 9.3.2021, v0.1
  */
class RadioButtonGroup[A](parentHierarchy: ComponentHierarchy, contextPointer: Changing[TextContext],
                          options: Vector[(A, LocalizedString)], override val valuePointer: EventfulPointer[A],
                          settings: RadioButtonGroupSettings = RadioButtonGroupSettings.default,
                          hotKeys: Map[A, Set[HotKey]] = Map())
	extends ReachComponentWrapper with Pool[Vector[A]] with InteractionWithPointer[A]
		with ManyFocusableWrapper
{
	// ATTRIBUTES	------------------------
	
	override val content = options.map { _._1 }
	
	private val (_wrapped, buttons) = Stack(parentHierarchy).withContext(contextPointer.value)
		.withSettings(settings.stackSettings).related
		.build(RadioButtonLine) { baseLineF =>
			// Creates a line for each option
			val lineContextPointer = {
				if (settings.axis == Y)
					contextPointer.mapWhile(parentHierarchy.linkPointer) { _.withTextExpandingToRight }
				else
					contextPointer
			}
			val lineF = baseLineF.withContextPointer(lineContextPointer).withSelectedColorRole(settings.selectedColorRole)
			val lines = options.map { case (item, text) =>
				lineF.withHotKeys(hotKeys.getOrElse(item, Set()))(valuePointer, item, text)
			}
			// Collects the radio buttons as additional results
			lines.map { _.parent } -> lines.map { _.result }
		}.parentAndResult
	
	/**
	  * Pointer that contains true while any button in this group has focus
	  */
	lazy val focusPointer = focusTargets.map { _.focusPointer }.reduceOption { _ || _ }.getOrElse(AlwaysFalse)
	
	
	// INITIAL CODE	------------------------
	
	// While focused, allows the user to change items with arrow keys
	if (buttons.size > 1) {
		addHierarchyListener { isAttached =>
			if (isAttached)
				KeyboardEvents += ArrowKeyListener
			else
				KeyboardEvents -= ArrowKeyListener
		}
	}
	
	
	// IMPLEMENTED	------------------------
	
	override protected def wrapped = _wrapped
	override protected def focusTargets = buttons
	
	override def hasFocus = focusPointer.value
	
	
	// NESTED	----------------------------
	
	private object ArrowKeyListener extends KeyStateListener
	{
		// ATTRIBUTES   --------------------
		
		override val keyStateEventFilter = KeyStateEvent.filter.pressed && KeyStateEvent.filter.anyArrow
		
		
		// IMPLEMENTED  --------------------
		
		override def handleCondition: FlagLike = focusPointer
		
		override def onKeyState(event: KeyStateEvent) = event.arrowAlong(settings.axis).foreach { direction =>
			if (moveFocusInside(direction.sign, forceFocusLeave = true))
				buttons.find { _.hasFocus }.foreach { _.select() }
		}
	}
}
