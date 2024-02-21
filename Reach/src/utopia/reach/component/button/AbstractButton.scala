package utopia.reach.component.button

import utopia.firmament.model.GuiElementStatus
import utopia.firmament.model.enumeration.GuiElementState.{Disabled, Focused}
import utopia.flow.view.mutable.eventful.EventfulPointer
import utopia.flow.view.template.eventful.{Changing, FlagLike}
import utopia.genesis.handling.event.keyboard.Key
import utopia.reach.component.template.ButtonLike
import utopia.reach.focus.FocusListener

/**
  * An abstract (immutable) implementation of the ButtonLike trait, intended to simplify sub-class implementation
  * @author Mikko Hilpinen
  * @since 21/02/2024, v1.3
  */
abstract class AbstractButton(settings: ButtonSettingsLike[_], triggerKeys: Set[Key] = ButtonLike.defaultTriggerKeys)
	extends ButtonLike
{
	// ATTRIBUTES   ------------------------
	
	override val enabledPointer: FlagLike = settings.enabledPointer
	
	private val baseStatePointer = EventfulPointer(GuiElementStatus.identity)
	override val statePointer: Changing[GuiElementStatus] = {
		if (enabledPointer.isAlwaysTrue)
			baseStatePointer.readOnly
		else
			baseStatePointer.mergeWith(enabledPointer) { (state, enabled) => state + (Disabled -> !enabled) }
	}
	
	override val focusId: Int = hashCode()
	override val focusPointer: FlagLike = statePointer.map { _ is Focused }
	override val focusListeners: Seq[FocusListener] =
		new ButtonDefaultFocusListener(baseStatePointer) +: settings.focusListeners
		
	
	// OTHER    ----------------------------
	
	/**
	  * Sets up this button's event-handling etc.
	  * Call this function from the sub-class once the other required properties
	  * (such as event handlers) have been set up.
	  */
	protected def setup(): Unit = setup(baseStatePointer, settings.hotKeys, triggerKeys)
}
