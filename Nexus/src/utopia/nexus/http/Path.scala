package utopia.nexus.http

import utopia.flow.collection.CollectionExtensions._

object Path
{
    // OPERATORS    ----------------------
    
    /**
     * Creates a path from a set of ordered strings
     */
    def apply(first: String, more: String*) = new Path(first +: more)
    
    
    // EXTENSIONS    ---------------------
    
    implicit class PathOption(val p: Option[Path]) extends AnyVal
    {
        /**
         * Appends this path with another path if this path is present. Otherwise simply returns 
         * the provided path
         */
        def /(path: Path) = p.map(_/path).getOrElse(path)
        
        /**
         * Appends this path with another element if this path is present. Otherwise creates 
         * a new path out of the element
         */
        def /(element: String) = p.map(_/element).getOrElse(Path(element))
    }
    
    
    // OTHER METHODS    ------------------
    
    /**
     * Parses a path from a string representation
     * @param pathString a string representation of a path. Eg. 'foo/bar'
     * @return the parsed path. None if there was no parts on the path
     */
    def parse(pathString: String) = 
    {
        val newParts = pathString.split("/").filterNot { _.isEmpty() }
        if (newParts.isEmpty) None else Some(Path(newParts.toVector))
    }
}

/**
 * Paths are used for determining a location of a resource on server side
 * @author Mikko Hilpinen
 * @since 22.8.2017
 */
case class Path(parts: Seq[String])
{
    // COMPUTED PROPERTIES    -----------------
    
    /**
     * The first string element in the path
     */
    def head = parts.head
    
    /**
     * The remaining portion of the path after the first element
     */
    def tail = drop()
    
    /**
     * The last element on the path
     */
    def lastElement = parts.last
    
    /**
     * The length / size of this path
     */
    def length = parts.size
    
    /**
     * This path as a complete url
     */
    def toServerUrl(implicit settings: ServerSettings) = settings.address + "/" + this
    
    
    // IMPLEMENTED METHODS    -----------------
    
    override def toString = parts.mkString("/")
    
    
    // OPERATORS    ---------------------------
    
    /**
     * Creates a new path with the specified path added to the end
     */
    def /(path: Path) = Path(parts ++ path.parts)
    
    /**
     * Creates a new path with the specified element appended to the end
     */
    def /(element: String): Path = Path.parse(element).map(/).getOrElse(this) 
    
    
    // OTHER METHODS    -----------------------
    
    /**
     * Creates a new path with the specified path prepended to the beginning
     */
    def prepend(path: Path) = Path(path.parts ++ parts)
    
    /**
     * Creates a new path with the specified element prepended to the beginning
     */
    def prepend(element: String): Path = Path.parse(element).map(prepend).getOrElse(this)
    
    /**
     * Drops the first n element(s) from this path and returns the result
     */
    def drop(n: Int = 1) = if (n >= parts.size) None else Some(Path(parts.drop(n)))
    
    /**
     * Drops the last n element(s) from this path and returns the result
     */
    def dropLast(n: Int = 1) = if (n >= parts.size) None else Some(Path(parts.dropRight(n)))
    
    /**
     * Finds the path before a specified path portion
     * @return the path portion before the specified path portion. None if this path doesn't 
     * contain the specified portion of if this path starts with the specified portion
     */
    def before(part: Path) = 
    {
        val partIndex = parts.indexOfSlice(part.parts)
        if (partIndex <= 0)
            None
        else
            Some(Path(parts.take(partIndex)))
    }
    
    /**
     * Finds the path after a specified path portion
     * @return the path portion after the specified path portion. None if this path doesn't 
     * contain the specified portion of if this path ends with the specified portion
     */
    def after(part: Path) = 
    {
        val partIndex = parts.indexOfSlice(part.parts)
        if (partIndex < 0)
            None
        else
        {
            val partEndIndex = partIndex + part.parts.size
            if (partEndIndex >= parts.size)
                None
            else
                Some(Path(parts.drop(partEndIndex)))
        }
    }
    
    /**
     * @param element A path element
     * @return The portion of this path that comes before the specified element. None if the specified element is not
     *         part of this path or if it was the first element in this path.
     */
    def before(element: String) =
        parts.optionIndexOf(element).filter { _ > 0 }.map { index => Path(parts.take(index)) }
    /**
     * @param element Searched element
     * @return A portion of this path that ends with the specified element. None if this path didn't contain specified
     *         element.
     */
    def until(element: String) = parts.optionIndexOf(element).map { index => Path(parts.take(index + 1)) }
}