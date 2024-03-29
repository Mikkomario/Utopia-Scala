package utopia.firmament.component.text

import utopia.firmament.localization.LocalizedString

/**
  * A common trait for components that present text and allow outside modifications to both content and styling
  * @author Mikko Hilpinen
  * @since 4.10.2020, Reflection v2
  */
trait MutableTextComponent extends MutableStyleTextComponent
{
	// ABSTRACT	------------------------
	
	/**
	  * @return Text that is currently displayed on this component
	  */
	def text: LocalizedString
	def text_=(newText: LocalizedString): Unit
}
