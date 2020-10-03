package utopia.reflection.test

import utopia.flow.async.Loop
import utopia.flow.util.FileExtensions._
import utopia.flow.util.StringExtensions._
import utopia.flow.util.TimeExtensions._
import utopia.genesis.image.Image
import utopia.reflection.color.ColorRole.Primary
import utopia.reflection.component.context.TextContext
import utopia.reflection.component.swing.input.{TagView, TypeOrSearch}
import utopia.reflection.container.swing.layout.multi.Stack
import utopia.reflection.container.swing.window.Frame
import utopia.reflection.container.swing.window.WindowResizePolicy.Program
import utopia.reflection.image.SingleColorIcon
import utopia.reflection.shape.LengthExtensions._
import utopia.reflection.util.{AwtEventThread, SingleFrameSetup}

/**
  * Tests typeOrSearch and TagView components
  * @author Mikko Hilpinen
  * @since 27.9.2020, v1.3
  */
object TypeOrSearchTest extends App
{
	import TestContext._
	
	AwtEventThread.debugMode = true
	
	val addIcon = new SingleColorIcon(Image.readFrom("Reflection/test-images/add.png").get)
	val closeIcon = new SingleColorIcon(Image.readFrom("Reflection/test-images/close.png").get * 0.8)
	
	implicit val context: TextContext = baseContext.inContextWithBackground(colorScheme.gray.light).forTextComponents()
	
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
		if (w.nonEmpty)
			words.filter { _.containsIgnoreCase(w) }
		else
			words
	}
	val tags = TagView.withRemovalEnabled(480, closeIcon)
	
	search.contentPointer.addListener { w => tags.content = w.newValue.map { _ -> context.color(Primary) } }
	tags.contentPointer.addListener { event =>
		if (event.newValue.size < event.oldValue.size)
			search --= event.oldValue.map { _._1 }.toSet -- event.newValue.map { _._1 }.toSet
	}
	
	val content = Stack.buildColumnWithContext() { s =>
		s += search
		s += tags
	}.framed(margins.medium.any, context.containerBackground)
	val frame = Frame.windowed(content, "Type or Search Test", Program)
	
	new SingleFrameSetup(actorHandler, frame).start()
	
	val testLoop = Loop(1.seconds) { println(AwtEventThread.debugString) }
	testLoop.registerToStopOnceJVMCloses()
	testLoop.startAsync()
}
