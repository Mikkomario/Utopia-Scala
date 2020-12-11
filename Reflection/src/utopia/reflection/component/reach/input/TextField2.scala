package utopia.reflection.component.reach.input

import utopia.flow.datastructure.mutable.PointerWithEvents
import utopia.flow.event.{ChangingLike, Fixed}
import utopia.flow.util.StringExtensions._
import utopia.genesis.color.Color
import utopia.genesis.shape.Axis.X
import utopia.reflection.color.ColorRole.Secondary
import utopia.reflection.color.ColorRole
import utopia.reflection.component.context.TextContextLike
import utopia.reflection.component.drawing.immutable.TextDrawContext
import utopia.reflection.component.reach.hierarchy.ComponentHierarchy
import utopia.reflection.component.reach.label.ViewTextLabel
import utopia.reflection.component.reach.template.{MutableFocusableWrapper, ReachComponentWrapper}
import utopia.reflection.component.reach.wrapper.Open
import utopia.reflection.component.template.input.InputWithPointer
import utopia.reflection.image.SingleColorIcon
import utopia.reflection.localization.LocalString._
import utopia.reflection.localization.{DisplayFunction, LocalizedString, Localizer}
import utopia.reflection.shape.stack.modifier.MaxBetweenLengthModifier
import utopia.reflection.shape.stack.StackLength
import utopia.reflection.shape.Alignment
import utopia.reflection.text.Regex
import utopia.reflection.util.ComponentCreationDefaults

import scala.concurrent.duration.Duration

object TextField2
{

}

class TextFieldFactory2(parentHierarchy: ComponentHierarchy)
{

}

case class ContextualTextFieldFactory2[+N <: TextContextLike](parentHierarchy: ComponentHierarchy, context: N)
{
	private implicit val c: TextContextLike = context
	
	def apply[A](defaultWidth: StackLength,
				 fieldNamePointer: ChangingLike[LocalizedString] = Fixed(LocalizedString.empty),
				 promptPointer: ChangingLike[LocalizedString] = Fixed(LocalizedString.empty),
				 hintPointer: ChangingLike[LocalizedString] = Fixed(LocalizedString.empty),
				 errorMessagePointer: ChangingLike[LocalizedString] = Fixed(LocalizedString.empty),
				 textPointer: PointerWithEvents[String] = new PointerWithEvents(""),
				 leftIconPointer: ChangingLike[Option[SingleColorIcon]] = Fixed(None),
				 rightIconPointer: ChangingLike[Option[SingleColorIcon]] = Fixed(None),
				 enabledPointer: ChangingLike[Boolean] = Fixed(true),
				 selectionStylePointer: ChangingLike[ColorRole] = Fixed(Secondary),
				 highlightStylePointer: ChangingLike[Option[ColorRole]] = Fixed(None),
				 focusColorRole: ColorRole = Secondary, hintScaleFactor: Double = TextField.defaultHintScaleFactor,
				 caretBlinkFrequency: Duration = ComponentCreationDefaults.caretBlinkFrequency,
				 inputFilter: Option[Regex] = None,
				 resultFilter: Option[Regex] = None, maxLength: Option[Int] = None,
				 inputValidation: Option[A => LocalizedString] = None, fillBackground: Boolean = true,
				 showCharacterCount: Boolean = false)
				(parseResult: Option[String] => A) =
		new TextField2[A](parentHierarchy, defaultWidth, fieldNamePointer, promptPointer, hintPointer,
			errorMessagePointer, textPointer, leftIconPointer, rightIconPointer, enabledPointer, selectionStylePointer,
			highlightStylePointer, focusColorRole, hintScaleFactor, caretBlinkFrequency, inputFilter, resultFilter,
			maxLength, inputValidation, fillBackground, showCharacterCount)(parseResult)
	
	def forString(defaultWidth: StackLength,
				  fieldNamePointer: ChangingLike[LocalizedString] = Fixed(LocalizedString.empty),
				  promptPointer: ChangingLike[LocalizedString] = Fixed(LocalizedString.empty),
				  hintPointer: ChangingLike[LocalizedString] = Fixed(LocalizedString.empty),
				  errorMessagePointer: ChangingLike[LocalizedString] = Fixed(LocalizedString.empty),
				  textPointer: PointerWithEvents[String] = new PointerWithEvents(""),
				  leftIconPointer: ChangingLike[Option[SingleColorIcon]] = Fixed(None),
				  rightIconPointer: ChangingLike[Option[SingleColorIcon]] = Fixed(None),
				  enabledPointer: ChangingLike[Boolean] = Fixed(true),
				  selectionStylePointer: ChangingLike[ColorRole] = Fixed(Secondary),
				  highlightStylePointer: ChangingLike[Option[ColorRole]] = Fixed(None),
				  focusColorRole: ColorRole = Secondary, hintScaleFactor: Double = TextField.defaultHintScaleFactor,
				  caretBlinkFrequency: Duration = ComponentCreationDefaults.caretBlinkFrequency,
				  inputFilter: Option[Regex] = None,
				  resultFilter: Option[Regex] = None, maxLength: Option[Int] = None,
				  inputValidation: Option[String => LocalizedString] = None, fillBackground: Boolean = true,
				  showCharacterCount: Boolean = false) =
		apply[String](defaultWidth, fieldNamePointer, promptPointer, hintPointer, errorMessagePointer, textPointer,
			leftIconPointer, rightIconPointer, enabledPointer, selectionStylePointer, highlightStylePointer,
			focusColorRole, hintScaleFactor, caretBlinkFrequency, inputFilter, resultFilter, maxLength,
			inputValidation, fillBackground, showCharacterCount) { _ getOrElse "" }
	
	// initialValue: Option[Int] = None,
	//			   minValue: Int = Int.MinValue, maxValue: Int = Int.MaxValue
	/*
	def forInt(defaultWidth: StackLength,
			   fieldNamePointer: ChangingLike[LocalizedString] = Fixed(LocalizedString.empty),
			   promptPointer: ChangingLike[LocalizedString] = Fixed(LocalizedString.empty),
			   hintPointer: ChangingLike[LocalizedString] = Fixed(LocalizedString.empty),
			   errorMessagePointer: ChangingLike[LocalizedString] = Fixed(LocalizedString.empty),
			   leftIconPointer: ChangingLike[Option[SingleColorIcon]] = Fixed(None),
			   rightIconPointer: ChangingLike[Option[SingleColorIcon]] = Fixed(None),
			   enabledPointer: ChangingLike[Boolean] = Fixed(true), initialValue: Option[Int] = None,
			   minValue: Int = Int.MinValue, maxValue: Int = Int.MaxValue,
			   selectionStylePointer: ChangingLike[ColorRole] = Fixed(Secondary),
			   highlightStylePointer: ChangingLike[Option[ColorRole]] = Fixed(None),
			   focusColorRole: ColorRole = Secondary, hintScaleFactor: Double = TextField.defaultHintScaleFactor,
			   caretBlinkFrequency: Duration = ComponentCreationDefaults.caretBlinkFrequency,
			   fillBackground: Boolean = true, allowAutoHint: Boolean = true) =
	{
		// Only accepts integer numbers
		val inputFilter = if (minValue < 0) Regex.numericParts else Regex.digit
		val resultFilter = if (minValue < 0) Regex.numeric else Regex.numericPositive
		
		/*
		forNumbers(minValue, maxValue, inputFilter, resultFilter, fieldNamePointer, promptPointer, hintPointer,
			errorMessagePointer, highlightStylePointer, enabledPointer, initialValue, hintScaleFactor,
			caretBlinkFrequency, allowAutoHint && minValue > Int.MinValue && minValue != 0,
			allowAutoHint && maxValue < Int.MaxValue, fillBackground) { _.int }
		 */
	}*/
}

/**
  * Used for requesting text input from the user
  * @author Mikko Hilpinen
  * @since 14.11.2020, v2
  */
class TextField2[A](parentHierarchy: ComponentHierarchy, defaultWidth: StackLength,
					fieldNamePointer: ChangingLike[LocalizedString] = Fixed(LocalizedString.empty),
					promptPointer: ChangingLike[LocalizedString] = Fixed(LocalizedString.empty),
					hintPointer: ChangingLike[LocalizedString] = Fixed(LocalizedString.empty),
					errorMessagePointer: ChangingLike[LocalizedString] = Fixed(LocalizedString.empty),
					textPointer: PointerWithEvents[String] = new PointerWithEvents(""),
					leftIconPointer: ChangingLike[Option[SingleColorIcon]] = Fixed(None),
					rightIconPointer: ChangingLike[Option[SingleColorIcon]] = Fixed(None),
					enabledPointer: ChangingLike[Boolean] = Fixed(true),
					selectionStylePointer: ChangingLike[ColorRole] = Fixed(Secondary),
					highlightStylePointer: ChangingLike[Option[ColorRole]] = Fixed(None),
					focusColorRole: ColorRole = Secondary, hintScaleFactor: Double = TextField.defaultHintScaleFactor,
					caretBlinkFrequency: Duration = ComponentCreationDefaults.caretBlinkFrequency,
					inputFilter: Option[Regex] = None,
					resultFilter: Option[Regex] = None, maxLength: Option[Int] = None,
					inputValidation: Option[A => LocalizedString] = None, fillBackground: Boolean = true,
					showCharacterCount: Boolean = false)
				   (parseResult: Option[String] => A)(implicit context: TextContextLike)
	extends ReachComponentWrapper with InputWithPointer[A, ChangingLike[A]] with MutableFocusableWrapper
{
	// ATTRIBUTES	------------------------------------------
	
	override val valuePointer = resultFilter match
	{
		case Some(filter) => textPointer.map { text => parseResult(filter.filter(text).notEmpty) }
		case None => textPointer.map { text => parseResult(text.notEmpty) }
	}
	
	// Uses either the outside error message, an input validator, both or neither as the error message pointer
	private val actualErrorPointer = inputValidation match
	{
		case Some(validation) =>
			val validationErrorPointer = valuePointer.map(validation)
			errorMessagePointer.notFixedWhere { _.isEmpty } match
			{
				case Some(outsideError) => outsideError.mergeWith(validationErrorPointer) { (default, validation) =>
					default.notEmpty.getOrElse(validation) }
				case None => validationErrorPointer
			}
		case None => errorMessagePointer
	}
	
	private val isEmptyPointer = textPointer.map { _.isEmpty }
	
	private val _wrapped = Field(parentHierarchy).withContext(context).apply[EditableTextLabel](isEmptyPointer,
		fieldNamePointer, promptPointer, hintPointer, actualErrorPointer, leftIconPointer, rightIconPointer,
		context.textInsets.total / 2, highlightStylePointer, focusColorRole, hintScaleFactor,
		fillBackground) { (fc, tc) =>
		
		val stylePointer = fc.textStylePointer.map { _.expandingHorizontally }
		val selectedBackgroundPointer = fc.backgroundPointer.mergeWith(selectionStylePointer) { (bg, c) =>
			tc.colorScheme(c).forBackgroundPreferringLight(bg) }
		val selectedTextColorPointer = selectedBackgroundPointer.map { _.defaultTextColor }
		val caretColorPointer = fc.backgroundPointer.mergeWith(selectedBackgroundPointer) { Vector[Color](_, _) }
			.mergeWith(selectionStylePointer) { (bgs, c) => tc.colorScheme(c).bestAgainst(bgs): Color }
		val caretWidth = (context.margins.verySmall / 2) max 1
		
		val label = EditableTextLabel(fc.parentHierarchy).apply(tc.actorHandler, stylePointer,
			selectedTextColorPointer, selectedBackgroundPointer.map { Some(_) }, caretColorPointer, caretWidth,
			caretBlinkFrequency, textPointer, inputFilter, maxLength,
			enabledPointer, allowSelectionWhileDisabled = false, tc.allowLineBreaks, tc.allowTextShrink)
		label.addFocusListener(fc.focusListener)
		fc.promptDrawers.foreach(label.addCustomDrawer)
		
		label
	} { (fc, tc) =>
		if (showCharacterCount)
			maxLength.map { maxLength =>
				implicit val localizer: Localizer = tc.localizer
				val countStylePointer = fc.backgroundPointer.map { background => TextDrawContext(fc.font,
					background.textColorStandard.hintTextColor, Alignment.Right, tc.textInsets * hintScaleFactor) }
				val textLengthPointer = textPointer.map { _.length }
				Open.using(ViewTextLabel) { _.apply(textLengthPointer, countStylePointer,
					DisplayFunction.functionToDisplayFunction[Int] { length =>
						s"%i / %i".noLanguage.localized.interpolated(Vector(length, maxLength)) },
					allowLineBreaks = false, allowTextShrink = true) }(parentHierarchy.top)
			}
		else
			None
	}
	
	
	// INITIAL CODE	------------------------------------------
	
	// Will not shrink below the default width
	_wrapped.wrappedField.addConstraintOver(X)(MaxBetweenLengthModifier(defaultWidth))
	
	
	// IMPLEMENTED	------------------------------------------
	
	override protected def wrapped = _wrapped
	
	override protected def focusable = _wrapped.wrappedField
}
