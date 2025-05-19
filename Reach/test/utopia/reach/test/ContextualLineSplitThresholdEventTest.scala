package utopia.reach.test

import ReachTestContext._
import utopia.firmament.context.text.VariableTextContext
import utopia.flow.view.mutable.Pointer
import utopia.paradigm.color.Color

/**
  * Makes sure line split threshold pointer changes trigger text draw context pointer changes in variable text context
  * @author Mikko Hilpinen
  * @since 19.05.2025, v1.5
  */
object ContextualLineSplitThresholdEventTest extends App
{
	private val splitThresholdP = Pointer.eventful(200.0)
	private val context = VariableTextContext(baseContext.against(Color.white).toVariableContext)
		.withLineSplitThresholdPointer(splitThresholdP)
	
	private val textContextP = context.textDrawContextPointer
	private var lastTextContextLineSplit: Option[Double] = None
	
	textContextP.addListener { e => lastTextContextLineSplit = e.newValue.lineSplitThreshold }
	
	// Modifies the threshold => Expects a change in context
	println("Changing line split threshold")
	splitThresholdP.value = 300
	
	assert(lastTextContextLineSplit.contains(300.0), lastTextContextLineSplit)
	
	println("Done!")
}
