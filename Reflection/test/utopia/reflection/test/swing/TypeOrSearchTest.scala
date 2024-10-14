package utopia.reflection.test.swing

import utopia.firmament.awt.AwtEventThread
import utopia.firmament.context.text.StaticTextContext
import utopia.firmament.image.SingleColorIcon
import utopia.firmament.model.enumeration.WindowResizePolicy.Program
import utopia.firmament.model.stack.LengthExtensions._
import utopia.flow.async.process.Loop
import utopia.flow.parse.file.FileExtensions._
import utopia.flow.time.TimeExtensions._
import utopia.flow.util.StringExtensions._
import utopia.genesis.image.Image
import utopia.paradigm.color.ColorRole.{Gray, Primary}
import utopia.reflection.component.swing.input.{TagView, TypeOrSearch}
import utopia.reflection.container.swing.layout.multi.Stack
import utopia.reflection.container.swing.window.Frame
import utopia.reflection.test.TestContext
import utopia.reflection.util.SingleFrameSetup

import scala.concurrent.Future

/**
  * Tests typeOrSearch and TagView components
  * @author Mikko Hilpinen
  * @since 27.9.2020, v1.3
  */
object TypeOrSearchTest extends App
{
	
	import TestContext._
	
	AwtEventThread.debugMode = true
	
	val addIcon = SingleColorIcon(Image.readFrom("Reflection/test-images/add.png").get)
	val closeIcon = SingleColorIcon(Image.readFrom("Reflection/test-images/close.png").get * 0.8)
	
	implicit val context: StaticTextContext = baseContext.against(colorScheme(Gray).light).forTextComponents
	
	/* TODO: Add following features
		- Submit on enter
		- Fix selection and allow traversal with arrow keys
		- Disable animations or fix label animation (now too wide)
		- Make animations in tag view optional
		- Display add prompt when not content to search
		- Button size in tags is not correct
	 */
	var words = Vector("Aamu", "Apina", "Banana", "David", "Johnson", "Clear", "Cost", "Calculated", "Oregano",
		"Voisilmä", "Namu", "Kolibri", "Korianteri", "Salmiakki", "Vanukas", "Vapaamatkustaja", "Tuhatjalkainen",
		"Turhake", "Turjake").sorted
	val search = TypeOrSearch.apply(320, addButtonIcon = Some(addIcon), selectButtonText = "Select",
		optimalSelectionAreaLength = Some(560), textFieldPrompt = "Type or Search") { w =>
		Future { if (w.nonEmpty) words.filter { _.containsIgnoreCase(w) } else words }
	}
	val tags = TagView.withRemovalEnabled(480, closeIcon)
	
	search.contentPointer.addContinuousListener { w => tags.content = w.newValue.map { _ -> context.color(Primary) } }
	tags.contentPointer.addContinuousListener { event =>
		if (event.newValue.size < event.oldValue.size)
			search --= event.oldValue.map { _._1 }.toSet -- event.newValue.map { _._1 }.toSet
	}
	
	val content = Stack.buildColumnWithContext() { s =>
		s += search
		s += tags
	}.framed(margins.medium.any, context.background)
	val frame = Frame.windowed(content, "Type or Search Test", Program)
	
	new SingleFrameSetup(actorHandler, frame).start()
	
	Loop.regularly(1.seconds, waitFirst = true) { println(AwtEventThread.debugString) }
}
