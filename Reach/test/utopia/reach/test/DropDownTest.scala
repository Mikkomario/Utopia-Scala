package utopia.reach.test

import utopia.firmament.image.SingleColorIcon
import utopia.firmament.localization.LocalizedString
import utopia.firmament.model.stack.LengthExtensions._
import utopia.flow.collection.CollectionExtensions._
import utopia.flow.parse.file.FileExtensions._
import utopia.flow.view.immutable.eventful.Fixed
import utopia.flow.view.mutable.eventful.PointerWithEvents
import utopia.genesis.image.Image
import utopia.paradigm.shape.shape2d.Matrix2D
import utopia.reach.component.input.selection.DropDown
import utopia.reach.component.label.text.TextLabel
import utopia.reach.component.wrapper.Open
import utopia.reach.container.ReachCanvas2
import utopia.reach.container.multi.Stack
import utopia.reach.container.wrapper.Framing
import utopia.reach.window.ReachWindow

/**
  * A test application with drop down fields
  * @author Mikko Hilpinen
  * @since 23.12.2020, v0.1
  */
object DropDownTest extends App
{
	import ReachTestContext._
	
	val arrowImage = Image.readFrom("Reflection/test-images/arrow-back-48dp.png")
	arrowImage.logFailure
	val expandIcon = arrowImage.map { i => new SingleColorIcon(i.transformedWith(Matrix2D.quarterRotationCounterClockwise)) }
	val shrinkIcon = arrowImage.map { i => new SingleColorIcon(i.transformedWith(Matrix2D.quarterRotationClockwise)) }
	
	val items = Map("Fruits" -> Vector("Apple", "Banana", "Kiwi"), "Minerals" -> Vector("Diamond", "Ruby", "Sapphire"))
	
	val window = ReachWindow.contextual.apply(title = "Drop-Down Test") { hierarchy =>
		implicit val canvas: ReachCanvas2 = hierarchy.top
		Framing(hierarchy).buildFilledWithContext(baseContext, colors.gray.light, Stack)
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
	}
	
	window.setToCloseOnEsc()
	window.display()
	start()
}
