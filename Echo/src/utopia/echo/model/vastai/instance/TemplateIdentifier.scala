package utopia.echo.model.vastai.instance

/**
 * Represents a template used when creating an instance
 * @param id ID of this template
 * @param hashId Hash-based ID of this template
 * @param name Name of this template
 * @author Mikko Hilpinen
 * @since 24.02.2026, v1.5
 */
case class TemplateIdentifier(id: Long, hashId: String, name: String)
