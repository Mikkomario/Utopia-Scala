package utopia.reach.component.interactive.button

import utopia.firmament.context.text.StaticTextContext
import utopia.firmament.image.{ButtonImageSet, SingleColorIcon}
import utopia.firmament.localization.LocalizedString
import utopia.genesis.image.Image
import utopia.paradigm.color.ColorRole.Primary
import utopia.paradigm.color.{ColorRole, FromColorRoleFactory}
import utopia.reach.component.factory.ContextualComponentFactories.CCF
import utopia.reach.component.factory.contextual.TextContextualFactory
import utopia.reach.component.hierarchy.ComponentHierarchy
import utopia.reach.component.interactive.button.image._
import utopia.reach.component.interactive.button.text.TextButton
import utopia.reach.component.template.PartOfComponentHierarchy

/**
 * Provides factories for constructing different types of static buttons
 *
 * @author Mikko Hilpinen
 * @since 09.09.2025, v1.7
 */
object Button extends CCF[StaticTextContext, ButtonFactory]
{
	override def withContext(hierarchy: ComponentHierarchy, context: StaticTextContext): ButtonFactory =
		ButtonFactory(hierarchy, context)
}

case class ButtonFactory(hierarchy: ComponentHierarchy, context: StaticTextContext,
                         settings: ImageAndTextButtonSettings = ImageAndTextButtonSettings.default,
                         buttonColor: Option[ColorRole] = None)
	extends ImageAndTextButtonSettingsWrapper[ButtonFactory] with TextContextualFactory[ButtonFactory]
		with PartOfComponentHierarchy with FromColorRoleFactory[ButtonFactory]
{
	// COMPUTED ------------------------------
	
	private def image =
		ImageButton.withContext(hierarchy, context).withSettings(ImageButtonSettings.from(settings))
	
	private def imageAndText =
		ImageAndTextButton.withContext(hierarchy, context).withSettings(settings)(buttonColor.getOrElse(Primary))
	
	
	// IMPLEMENTED  --------------------------
	
	override def self: ButtonFactory = this
	
	override protected def withSettings(settings: ImageAndTextButtonSettings): ButtonFactory = copy(settings = settings)
	override def withContext(context: StaticTextContext): ButtonFactory = copy(context = context)
	
	override def apply(role: ColorRole): ButtonFactory = copy(buttonColor = Some(role))
	
	
	// OTHER    ------------------------------
	
	/**
	 * @param content Button content
	 * @param action Action triggered when this button is pressed
	 * @return A button containing the specified content
	 */
	def apply(content: ButtonContent)(action: => Unit): AbstractButton = {
		val factory = content.color match {
			case Some(color) => apply(color)
			case None => self
		}
		factory(content.icon, content.text)(action)
	}
	
	/**
	 * @param text text to display on this button
	 * @param action Action triggered when this button is pressed
	 * @return A new text-based button
	 */
	def apply(text: LocalizedString)(action: => Unit) =
		TextButton.withContext(hierarchy, context)(buttonColor.getOrElse(Primary))(text)(action)
	
	/**
	 * @param icon Button icon
	 * @param action Action triggered when this button is pressed
	 * @return A new icon-based button
	 */
	def apply(icon: SingleColorIcon)(action: => Unit) = image.icon(icon, buttonColor)(action)
	/**
	 * @param image Button image
	 * @param action Action triggered when this button is pressed
	 * @return A new image-based button
	 */
	def apply(image: Image)(action: => Unit) = this.image(image)(action)
	/**
	 * @param images Button image-set
	 * @param action Action triggered when this button is pressed
	 * @return A new image-based button
	 */
	def apply(images: ButtonImageSet)(action: => Unit) = image(images)(action)
	
	/**
	 * @param icon Button icon (may be empty)
	 * @param text Button text (may be empty, if icon is not empty)
	 * @param action Action triggered when this button is pressed
	 * @return Creates a button consisting of the specified icon and text
	 */
	def apply(icon: SingleColorIcon, text: LocalizedString)(action: => Unit): AbstractButton = {
		if (icon.isEmpty)
			apply(text)(action)
		else if (text.isEmpty)
			apply(icon)(action)
		else
			imageAndText(icon, text)(action)
	}
	/**
	 * @param image Button image (may be empty)
	 * @param text Button text (may be empty, if image is not empty)
	 * @param action Action triggered when this button is pressed
	 * @return Creates a button consisting of the specified image and text
	 */
	def apply(image: Image, text: LocalizedString)(action: => Unit): AbstractButton = {
		if (image.isEmpty)
			apply(text)(action)
		else if (text.isEmpty)
			apply(image)(action)
		else
			imageAndText(image, text)(action)
	}
	/**
	 * @param images Button icon
	 * @param text Button text (may be empty)
	 * @param action Action triggered when this button is pressed
	 * @return Creates a button consisting of the specified images and text
	 */
	def apply(images: ButtonImageSet, text: LocalizedString)(action: => Unit): AbstractButton = {
		if (text.isEmpty)
			apply(images)(action)
		else
			imageAndText(images, text)(action)
	}
}
