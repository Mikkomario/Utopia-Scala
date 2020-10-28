package utopia.reflection.component.template.text

/**
  * This is a commom trait for components that present text on a single line
  * @author Mikko Hilpinen
  * @since 24.4.2019, v1+
  */
trait SingleLineTextComponent2 extends TextComponent2
{
	// IMPLEMENTED	----------------------
	
	override def allowLineBreaks = false
}
