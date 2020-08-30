package utopia.reflection.test

import utopia.reflection.localization.LocalString._
import utopia.flow.async.ThreadPool
import utopia.reflection.shape.LengthExtensions._
import utopia.genesis.color.Color
import utopia.reflection.component.swing.template.AwtComponentRelated
import utopia.reflection.component.template.ComponentLike
import utopia.reflection.component.template.layout.stack.StackLeaf
import utopia.reflection.container.swing.Panel
import utopia.reflection.container.swing.window.{Dialog, Frame}
import utopia.reflection.shape.stack.StackSize

import scala.concurrent.ExecutionContext

/**
  * This is a test implementation for dialogs
  * @author Mikko Hilpinen
  * @since 8.5.2019, v1+
  */
object DialogTest extends App
{
	implicit val exc: ExecutionContext = new ThreadPool("Reflection").executionContext
	
	private class ContentPanel(override val stackSize: StackSize) extends Panel[ComponentLike with AwtComponentRelated] with StackLeaf
	{
		background = Color.white
		
		override def stackId = hashCode()
		
		override def updateLayout() = ()
		override def resetCachedSize() = ()
	}
	
	private implicit val language: String = "en"
	private val frame = Frame.windowed(new ContentPanel(640.any x 480.any), "Frame".local.localizationSkipped)
	frame.setToExitOnClose()
	
	private val dialog = new Dialog(frame.component, new ContentPanel(320.any x 240.any), "Dialog".local.localizationSkipped)
	dialog.closeFuture.foreach { u => frame.background = Color.yellow }
	
	frame.isVisible = true
	dialog.isVisible = true
}
