package utopia.flow.util

/**
 * This object contains an extension that allows safer handling of java-originated objects 
 * that may possibly contain null values. 
 * Only import this object in files where java objects are handled.
 * @author Mikko Hilpinen
 * @since 28.8.2017
 */
object NullSafe
{
    implicit class NullOption[T <: Object](val obj: T) extends AnyVal
    {
        /**
         * Performs a null check and returns an optional non-null value. None is returned for 
         * null values.
         */
        def toOption = Option(obj)
    }
}