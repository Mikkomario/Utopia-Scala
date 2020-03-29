package utopia.flow.generic

import utopia.flow.datastructure.template.Model
import utopia.flow.datastructure.template.Property
import utopia.flow.parse.JSONReader

import scala.util.Try

/**
 * This trait is extended by instance factories that can convert model data into object data. 
 * The factory may make assumptions about the type of model data and may give more sensible results 
 * with other models than with others.
 */
trait FromModelFactory[+A]
{
    // ABSTRACT METHODS    ----------------------
    
    /**
     * Parses an instance by reading the data from a model instance
     * @param model Model data is parsed from
     * @return an instance parsed from model data. Failure if no instance could be parsed.
     */
    def apply(model: Model[Property]): Try[A]
    
    
    // OTHER METHODS   --------------------------
    
    /**
     * Parses an instance from a JSON string. Returns none if either the JSON string couldn't be 
     * parsed or if the instance couldn't be parsed from read data.
     */
    def fromJSON(json: String) = JSONReader(json).map(v => apply(v.getModel)).flatten
}