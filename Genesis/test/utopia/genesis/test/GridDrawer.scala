package utopia.genesis.test

import utopia.genesis.graphics.{DrawSettings, Drawer3, StrokeSettings}
import utopia.genesis.handling.Drawable2
import utopia.inception.handling.mutable.Handleable
import utopia.paradigm.shape.shape2d.{Line, Size, Vector2D}

import java.awt.Color

/**
 * This object simply draws a gird to the center of the game world
 * @author Mikko Hilpinen
 * @since 25.2.2017
 */
class GridDrawer(worldSize: Size, val squareSize: Size) extends Drawable2 with Handleable
{
    // ATTRIBUTES    -----------------
    
    private implicit val ds: DrawSettings = StrokeSettings(Color.lightGray)
    
    /**
     * How many squares there are on each axis
     */
    val squareAmounts = (worldSize / squareSize).floor
    /**
     * The size of the grid
     */
    private val size =  squareSize * squareAmounts
    /**
     * The position of the top left corner of the grid
     */
    private val position = ((worldSize - size) / 2).toPoint
    
    
    // IMPLEMENTED METHODS    --------
    
    def draw(drawer: Drawer3) =
    {
        for (x <- 0 to squareAmounts.x.toInt) {
            drawer.draw(Line.ofVector(squarePosition(x, 0).toVector.toPoint, size.toVector.yProjection))
        }
        for (y <- 0 to squareAmounts.y.toInt) {
            drawer.draw(Line.ofVector(squarePosition(0, y).toVector.toPoint, size.toVector.xProjection))
        }
    }
    
    
    // OTHER METHODS    -------------
    
    /**
     * The top left corner of a square in the grid
     * @param x The x-index of the square
     * @param y The y-index of the square
     */
    def squarePosition(x: Int, y: Int) = position + squareSize * Vector2D(x, y)
    
    /**
     * The center point of a square in the grid
     * @param x the x-index of the square
     * @param y the y-index of the square
     */
    def squareCenter(x: Int, y: Int) = squarePosition(x, y) + squareSize * 0.5
}