package utopia.genesis.graphics

/**
  * Provides a read-only access to a graphics instance. Intended for transformation and font metric reading.
  * @author Mikko Hilpinen
  * @since 30.1.2022, v2.6.3
  */
class GraphicsContext(protected override val graphics: LazyGraphics) extends GraphicsContextLike[GraphicsContext]
{
	override protected def withGraphics(newGraphics: LazyGraphics) = new GraphicsContext(graphics)
}
