package utopia.reflection.test.swing

import utopia.paradigm.color.Color
import utopia.reflection.component.swing.template.AwtComponentRelated
import utopia.reflection.component.template.ReflectionComponentLike
import utopia.reflection.component.template.layout.stack.ReflectionStackLeaf
import utopia.reflection.container.swing.Panel
import utopia.reflection.container.swing.window.{Dialog, Frame}
import utopia.firmament.localization.LocalString._
import utopia.firmament.model.stack.LengthExtensions._
import utopia.firmament.model.stack.StackSize
import utopia.reflection.test.TestContext._

/**
  * This is a test implementation for dialogs
  * @author Mikko Hilpinen
  * @since 8.5.2019, v1+
  */
object DialogTest extends App
{
	private class ContentPanel(override val stackSize: StackSize)
		extends Panel[ReflectionComponentLike with AwtComponentRelated] with ReflectionStackLeaf
	{
		background = Color.white

		override def stackId = hashCode()

		override def updateLayout() = ()

		override def resetCachedSize() = ()
	}

	private val frame = Frame.windowed(new ContentPanel(640.any x 480.any), "Frame".local.localizationSkipped)
	frame.setToExitOnClose()

	private val dialog = new Dialog(frame.component, new ContentPanel(320.any x 240.any), "Dialog".local.localizationSkipped)
	dialog.closeFuture.foreach { _ => frame.background = Color.yellow }

	frame.visible = true
	dialog.visible = true
}
