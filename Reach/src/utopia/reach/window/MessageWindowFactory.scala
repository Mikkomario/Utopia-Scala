package utopia.reach.window

import utopia.firmament.context.TextContext
import utopia.firmament.localization.LocalizedString
import utopia.firmament.model.WindowButtonBlueprint
import utopia.flow.view.immutable.eventful.AlwaysTrue
import utopia.reach.component.factory.ContextualMixed
import utopia.reach.component.label.text.TextLabel

/**
  * A common trait for window factories that produce interactive message or question windows.
  * No window has input components, other than the buttons it contains.
  * @author Mikko Hilpinen
  * @since 17.11.2022, v0.5
  */
trait MessageWindowFactory[A] extends InteractionWindowFactory[A]
{
	// ABSTRACT ---------------------------
	
	/**
	  * @return Message to display. May span multiple lines.
	  */
	def message: LocalizedString
	
	/**
	  * @return The context used in the message area
	  */
	protected def messageContext: TextContext
	
	/**
	  * @return Blueprints for the buttons displayed on this window
	  */
	protected def buttonBlueprints: Vector[WindowButtonBlueprint[A]]
	
	
	// IMPLEMENTED  ----------------------
	
	override protected def createContent(factories: ContextualMixed[TextContext]) = {
		// The main content is simply a text label
		val content = factories.withContext(messageContext)(TextLabel).apply(message)
		(content, buttonBlueprints, AlwaysTrue)
	}
}
