package utopia.reach.test

import utopia.firmament.image.SingleColorIcon
import utopia.flow.parse.file.FileExtensions._
import utopia.flow.collection.CollectionExtensions._
import utopia.flow.view.immutable.eventful.Fixed
import utopia.flow.view.mutable.eventful.PointerWithEvents
import utopia.paradigm.generic.ParadigmDataType
import utopia.genesis.image.Image
import utopia.paradigm.shape.shape2d.Matrix2D
import utopia.reach.component.input.selection.DropDown
import utopia.reach.component.label.text.TextLabel
import utopia.reach.component.wrapper.Open
import utopia.reach.container.multi.stack.Stack
import utopia.reach.container.ReachCanvas
import utopia.reach.container.wrapper.Framing
import utopia.reflection.container.swing.window.Frame
import utopia.firmament.model.enumeration.WindowResizePolicy.Program
import utopia.firmament.localization.LocalizedString
import utopia.firmament.model.stack.LengthExtensions._
import utopia.reflection.util.SingleFrameSetup

/**
  * A test application with drop down fields
  * @author Mikko Hilpinen
  * @since 23.12.2020, v0.1
  */
object DropDownTest extends App
{
	ParadigmDataType.setup()
	System.setProperty("sun.java2d.noddraw", true.toString)
	
	import utopia.reflection.test.TestContext._
	import TestCursors._
	
	val arrowImage = Image.readFrom("Reflection/test-images/arrow-back-48dp.png")
	arrowImage.failure.foreach { _.printStackTrace() }
	val expandIcon = arrowImage.map { i => new SingleColorIcon(i.transformedWith(Matrix2D.quarterRotationCounterClockwise)) }
	val shrinkIcon = arrowImage.map { i => new SingleColorIcon(i.transformedWith(Matrix2D.quarterRotationClockwise)) }
	
	val items = Map("Fruits" -> Vector("Apple", "Banana", "Kiwi"), "Minerals" -> Vector("Diamond", "Ruby", "Sapphire"))
	
	val canvas = ReachCanvas(cursors) { hierarchy =>
		implicit val canvas: ReachCanvas = hierarchy.top
		Framing(hierarchy).buildFilledWithContext(baseContext, colorScheme.gray.light, Stack)
			.apply(margins.medium.any.square) { stackF =>
				stackF.mapContext { _.forTextComponents }.build(DropDown).column(areRelated = true) { ddF =>
					val selectedCategoryPointer = new PointerWithEvents[Option[String]](None)
					val selectedItemPointer = new PointerWithEvents[Option[String]](None)
					
					selectedItemPointer.addListener { e => println(e) }
					
					Vector(
						ddF.simple(Fixed(items.keys.toVector.sorted), selectedCategoryPointer, expandIcon.toOption,
							shrinkIcon.toOption, fieldNamePointer = Fixed("Category"),
							promptPointer = Fixed("Select One")),
						ddF.simple(selectedCategoryPointer.map {
							case Some(category) => items(category)
							case None => Vector()
						}, selectedItemPointer, expandIcon.toOption, shrinkIcon.toOption,
							fieldNamePointer = selectedCategoryPointer.map
							{
								case Some(category) => category
								case None => "Item"
							}, hintPointer = selectedCategoryPointer.map {
								case Some(_) => LocalizedString.empty
								case None => "Select category first"
							},
							promptPointer = Fixed("Select One"),
							noOptionsView = Some(Open.using(TextLabel) { _.withContext(ddF.context).apply(
								"Please select a category first", isHint = true) }))
					)
				}
			}
	}.parent
	
	val frame = Frame.windowed(canvas, "Reach Test", Program)
	frame.setToCloseOnEsc()
	new SingleFrameSetup(actorHandler, frame).start()
}
