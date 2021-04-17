package utopia.reflection.component.template.text

import utopia.reflection.localization.LocalizedString

/**
  * A common trait for components that present text and allow outside modifications to both content and styling
  * @author Mikko Hilpinen
  * @since 4.10.2020, v2
  */
trait MutableTextComponent extends MutableStyleTextComponent
{
	// ABSTRACT	------------------------
	
	def text_=(newText: LocalizedString): Unit
}
