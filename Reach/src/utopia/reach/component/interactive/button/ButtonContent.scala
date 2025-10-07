package utopia.reach.component.interactive.button

import utopia.firmament.image.SingleColorIcon
import utopia.firmament.localization.LocalizedString
import utopia.paradigm.color.{ColorRole, FromColorRoleFactory}

object ButtonContent
{
	// ATTRIBUTES   -----------------------
	
	/**
	 * Empty button content
	 */
	lazy val empty = apply()
	
	
	// OTHER    ---------------------------
	
	/**
	 * @param icon Displayed icon
	 * @return Button content with only the specified icon
	 */
	def apply(icon: SingleColorIcon): ButtonContent = apply(icon = icon, text = LocalizedString.empty, color = None)
	/**
	 * @param icon Displayed icon
	 * @param text Displayed text
	 * @return Button content with the specified icon & text
	 */
	def apply(icon: SingleColorIcon, text: LocalizedString): ButtonContent = apply(text, icon)
}

/**
 * Combines button text, icon and/or color
 *
 * @author Mikko Hilpinen
 * @since 09.09.2025, v1.7
 */
case class ButtonContent(text: LocalizedString = LocalizedString.empty, icon: SingleColorIcon = SingleColorIcon.empty,
                         color: Option[ColorRole] = None, settings: ButtonSettings = ButtonSettings.default)
	extends FromColorRoleFactory[ButtonContent] with ButtonSettingsWrapper[ButtonContent]
{
	// IMPLEMENTED  -------------------------
	
	override def apply(role: ColorRole): ButtonContent = copy(color = Some(role))
	
	override def withSettings(settings: ButtonSettings): ButtonContent = copy(settings = settings)
	
	
	// OTHER    -----------------------------
	
	/**
	 * @param text New text to assign
	 * @return Copy of this content with the specified text
	 */
	def withText(text: LocalizedString) = copy(text = text)
	/**
	 * @param icon New icon to assign
	 * @return Copy of this content with the specified icon
	 */
	def withIcon(icon: SingleColorIcon) = copy(icon = icon)
}