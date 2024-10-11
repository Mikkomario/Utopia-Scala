package utopia.firmament.context.text

import utopia.firmament.context.color.VariableColorContextLike
import utopia.firmament.model.stack.StackInsets
import utopia.flow.util.Mutate
import utopia.flow.view.immutable.eventful.Fixed
import utopia.flow.view.template.eventful.Changing
import utopia.genesis.text.Font

/**
  * Common trait for pointer-based text context implementations.
  * @author Mikko Hilpinen
  * @since 05.10.2024, v1.4
  */
// TODO: Add hintTextDrawContextPointer
trait VariableTextContextLike[+Repr] extends VariableColorContextLike[Repr, Repr] with TextContextCopyable[Repr]
{
	// ABSTRACT -------------------------------
	
	/**
	  * @param p Prompt font pointer to use
	  * @return Copy of this context using the specified prompt font pointer
	  */
	def withPromptFontPointer(p: Changing[Font]): Repr
	/**
	  * @param p Text insets pointer to use
	  * @return Copy of this context using the specified pointer
	  */
	def withTextInsetsPointer(p: Changing[StackInsets]): Repr
	/**
	  * @param p Line split threshold -pointer to use.
	  *          None if automated line-splitting should be disabled.
	  * @return Copy of this context using the specified pointer
	  */
	def withLineSplitThresholdPointer(p: Option[Changing[Double]]): Repr
	
	
	// IMPLEMENTED  ---------------------------
	
	override def withPromptFont(font: Font): Repr = withPromptFontPointer(Fixed(font))
	override def withTextInsets(insets: StackInsets): Repr = withTextInsetsPointer(Fixed(insets))
	override def withLineSplitThreshold(threshold: Option[Double]): Repr =
		withLineSplitThresholdPointer(threshold.map(Fixed.apply))
	
	override def mapTextInsets(f: StackInsets => StackInsets): Repr = mapTextInsetsPointer { _.map(f) }
	
	
	// OTHER    -------------------------------
	
	/**
	  * @param p Line split threshold -pointer to use
	  * @return Copy of this context using the specified pointer
	  */
	def withLineSplitThresholdPointer(p: Changing[Double]): Repr = withLineSplitThresholdPointer(Some(p))
	
	def mapPromptFontPointer(f: Mutate[Changing[Font]]) = withPromptFontPointer(f(promptFontPointer))
	def mapTextInsetsPointer(f: Mutate[Changing[StackInsets]]) = withTextInsetsPointer(f(textInsetsPointer))
	def mapLineSplitThresholdPointer(f: Mutate[Option[Changing[Double]]]) =
		withLineSplitThresholdPointer(f(lineSplitThresholdPointer))
	/**
	  * If a line split threshold pointer has been defined, modifies it
	  * @param f A mapping function for a line split threshold pointer
	  * @return Copy of this context with a modified line split threshold pointer.
	  *         This instance if no line split threshold pointer was defined.
	  */
	def mapLineSplitThresholdPointerIfDefined(f: Mutate[Changing[Double]]) = lineSplitThresholdPointer match {
		case Some(p) => withLineSplitThresholdPointer(f(p))
		case None => self
	}
}
