package utopia.reflection.component.swing.input

import utopia.reflection.component.context.{AnimationContextLike, BaseContextLike, ButtonContextLike, ScrollingContextLike}
import utopia.reflection.component.swing.button.{ButtonImageSet, ButtonLike}
import utopia.reflection.component.template.display.Refreshable
import utopia.reflection.container.swing.layout.multi.AnimatedStack
import utopia.reflection.container.swing.layout.multi.Stack.AwtStackable
import utopia.reflection.localization.LocalizedString
import utopia.reflection.shape.stack.StackInsets
import utopia.reflection.text.{Font, Prompt}
import utopia.reflection.shape.LengthExtensions._

import scala.concurrent.ExecutionContext

/**
  * A component with which the user may search and select from existing options or create a completely new option
  * @author Mikko Hilpinen
  * @since 22.9.2020, v1.3
  */
class SelectOrCreate[C <: AwtStackable with Refreshable[String]]
(parentContext: BaseContextLike, textFieldContext: ButtonContextLike, addButton: ButtonLike,
 optimalTextFieldWidth: Double, optimalSelectionAreaLength: Option[Double] = None,
 textFieldPrompt: Option[LocalizedString] = None)
(optionsForInput: String => Seq[String])(labelForOption: String => C)
(implicit scrollingContext: ScrollingContextLike, animationContext: AnimationContextLike, exc: ExecutionContext)
{
	// ATTRIBUTES   ----------------------------
	
	private val margin = textFieldContext.relatedItemsStackMargin
	
	private val textField = TextField.contextual(optimalTextFieldWidth.any.expanding,
		prompt = textFieldPrompt)(textFieldContext)
	private val optionsStack =
	{
		implicit val c: BaseContextLike = parentContext
		AnimatedStack.contextualColumn(cap = margin, itemsAreRelated = true)
	}
	
}
