package utopia.reflection.container.swing.window.interaction

import utopia.flow.view.template.eventful.Changing
import utopia.reflection.container.swing.layout.multi.Stack.AwtStackable
import utopia.reflection.localization.LocalizedString

/**
  * Used for creating rows in dialogs with input fields
  * @author Mikko Hilpinen
  * @since 4.5.2020, v1.2
  * @param fieldName Name of this input field
  * @param field The input field component
  * @param rowVisibilityPointer A pointer that determines whether this row should be displayed or not
  *                             (default = None = always visible)
  * @param spansWholeRow Whether the input field should be stretched to span the same width as the rest of the input
  *                      fields.
  */
class InputRowBlueprint(val fieldName: LocalizedString, val field: AwtStackable,
                        val rowVisibilityPointer: Option[Changing[Boolean]] = None, val spansWholeRow: Boolean = true)