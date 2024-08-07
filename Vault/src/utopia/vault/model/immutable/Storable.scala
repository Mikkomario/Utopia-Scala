package utopia.vault.model.immutable

import utopia.flow.collection.CollectionExtensions._
import utopia.flow.generic.model.immutable.{Constant, Model, Value}
import utopia.flow.generic.model.template
import utopia.flow.generic.model.template.{ModelConvertible, Property}
import utopia.flow.operator.equality.EqualsExtensions._
import utopia.vault.database.{Connection, DBException}
import utopia.vault.model.enumeration.BasicCombineOperator.And
import utopia.vault.model.enumeration.ComparisonOperator.Equal
import utopia.vault.model.enumeration.{BasicCombineOperator, ComparisonOperator}
import utopia.vault.model.template.HasTable
import utopia.vault.nosql.factory.row.FromRowFactory
import utopia.vault.nosql.factory.row.model.FromRowModelFactory
import utopia.vault.sql.{Condition, Delete, Insert, SqlSegment, SqlTarget, Update, Where}

object Storable
{
    // OTHER    ---------------------------
    
    /**
      * Wraps a model into a storable instance
      * @param table The table the model uses
      * @param model A model that contains data
      * @return A new storable instance based on the model
      */
    def apply(table: Table, model: template.ModelLike[Property]): Storable = new StorableWrapper(table, model)
    
    
    // NESTED   ------------------------
    
    private class StorableWrapper(override val table: Table, val model: template.ModelLike[Property])
        extends StorableWithFactory[Storable]
    {
        override lazy val factory = FromRowModelFactory(table)
        
        override def valueProperties = model.properties.map { c => c.name -> c.value }
    }
}

/**
 * Storable instances can be stored into a database table.
 * @author Mikko Hilpinen
 * @since 10.6.2017
 */
trait Storable extends ModelConvertible with HasTable
{
    // ABSTRACT PROPERTIES & METHODS    --------------
    
    /**
     * The model properties of this storable instance
     */
    def valueProperties: Iterable[(String, Value)]
    
    
    // COMPUTED PROPERTIES    ------------------------
    
    /**
     * The index of this storable instance. Index is the primary method way to identify the 
     * instance in database context.
     */
    def index = {
        table.primaryColumn.flatMap { primary =>
            valueProperties.findMap { case (name, value) => if (name ~== primary.name) Some(value) else None }
        }.getOrElse(Value.empty)
    }
    
    /**
     * A declaration that describes this instance. The declaration is based on the instance's table
     */
    def declaration = table.toModelDeclaration
    
    /**
     * A condition for finding the row matching this item's index (will never use NULL index)
     */
    def indexCondition = index.notEmpty.flatMap { i => table.primaryColumn.map { _ <=> i } }
    
    /**
      * @return A condition based on this storable instance. All DEFINED properties are included in the condition.
      * @throws NoSuchElementException If this instance didn't have a single defined property
      */
    def toCondition = makeCondition { _ <=> _ }
    
    
    // IMPLEMENTED  ----------------------------------
    
    override def toModel = Model(valueProperties, declaration.toPropertyFactoryWithoutDefaults(Constant))
    
    
    // OTHER METHODS    ------------------------------
    
    /**
      * Creates a condition based on this storable instance. All DEFINED (= non-empty) properties are included in the
      * resulting condition.
      * @param comparisonOperator Operator used when comparing the items (default = equal (<=>))
      * @param combineOperator Operator used when combining individual conditions (Default = And = &&)
      * @return A condition based on this storable instance and specified operators
      */
    def toConditionWithOperator(comparisonOperator: ComparisonOperator = Equal,
                                combineOperator: BasicCombineOperator = And) =
        makeCondition({ _.makeCondition(comparisonOperator, _) }, combineOperator)
    
    /**
     * Converts this storable instance's properties into a condition. The condition checks that 
     * each of the instances DEFINED properties match their value in the database. Does not include 
     * any null / empty properties and will only include specified properties.
      * @param firstLimitKey The first key that is used
      * @param moreLimitKeys More keys used in the condition
     * @return a condition based on this storable instance. None if the instance didn't contain 
     * any properties that could be used for forming a condition
     */
    def toConditionWith(firstLimitKey: String, moreLimitKeys: String*) = {
        val limitKeys = firstLimitKey +: moreLimitKeys
        val model = toModel
        val columns = if (limitKeys.isEmpty) table.columns else table.columns.filter {
            c => limitKeys.exists(c.name.equalsIgnoreCase) }
        val conditions = columns.flatMap { c =>
            val value = model(c.name)
            if (value.isEmpty) None else Some(c <=> value)
        }
        
        conditions.headOption.map { _ && conditions.drop(1) }
    }
    
    /**
     * Pushes the storable instance's data to the database using either insert or update. In case 
     * the instance doesn't have a specified index AND it's table uses indexing that is not 
     * auto-increment, cannot push data and returns an empty value.
     * @param writeNulls Whether empty / null values should be pushed to the database (on update). 
     * False by default, which means that columns will never be specifically set to null. Use 
     * true if you specifically want to set a column to null.
     * @return The existing or generated index of the instance. In case of auto-increment table, this index was
     * just generated.
     */
    def push(writeNulls: Boolean = false)(implicit connection: Connection) = {
        // Either inserts as a new row or updates an existing row
        if (update(writeNulls))
            index
        else
            insert()
    }
    
    /**
     * Pushes storable data to the database, but only allows updating of an existing row. No 
     * inserts will be made. This will only succeed if the instance has a defined index.
     * @param writeNulls whether null values should be updated to the database. Defaults to false.
     * @return whether the database was actually updated
     */
    def update(writeNulls: Boolean = false)(implicit connection: Connection) = {
        val update = indexCondition.map { cond => toUpdateStatement(writeNulls = writeNulls) + Where(cond) }
        update.exists { statement =>
            try { statement.execute().updatedRows }
            catch {
                case e: DBException => e.rethrow(s"Failed to update storable: $toJson")
            }
        }
    }
    
    /**
      * Performs an update based on this model, but applies a specific condition
      * @param condition Condition applied to this update
      * @param customTarget Sql target that's being updated. None if only this storable's table should be updated (default)
      * @param writeNulls Whether empty (null) values should be pushed to the DB (default = false)
      * @param writeIndex Whether index value (if present) should be pushed to the DB (default = false)
      * @param connection Database connection (implicit)
      * @return Number of updated rows
      */
    def updateWhere(condition: Condition, customTarget: Option[SqlTarget] = None, writeNulls: Boolean = false,
                    writeIndex: Boolean = false)(implicit connection: Connection) =
        connection(toUpdateStatement(customTarget, writeNulls, writeIndex) + Where(condition)).updatedRowCount
    
    /**
     * Creates an update sql segment based on this storable instance. This update segment can then 
     * be combined with a condition in order to update row data to match that of this storable instance.
      * @param customTarget Sql target that's being updated. None if only this storable's table should be updated (default).
     * @param writeNulls whether null / empty value assignments should be included in the update segment. 
     * Defaults to false. In general, this should only be used when row id is defined and you want to overwrite a row.
     * @param writeIndex whether index should specifically be included among the set column values 
     * (where applicable). Defaults to false.
     */
    def toUpdateStatement(customTarget: Option[SqlTarget] = None, writeNulls: Boolean = false, writeIndex: Boolean = false) =
    {
        val primaryColumn = table.primaryColumn
        val originalModel = if (writeNulls) toModel else toModel.withoutEmptyValues
        val updateModel = if (writeIndex || primaryColumn.isEmpty) 
                originalModel else originalModel - primaryColumn.get.name
        customTarget match {
            case Some(target) => Update(target, table, updateModel)
            case None => Update(table, updateModel)
        }
    }
    
    /**
     * Updates certain properties to the database
     * @param propertyNames the names of the properties that are updated / pushed to the database
     * @return whether any update was performed
     */
    def updateProperties(propertyNames: Iterable[String])(implicit connection: Connection) = {
        val update = indexCondition.map { cond => updateStatementForProperties(propertyNames) + Where(cond) }
        update.foreach { statement =>
            try { statement.execute() }
            catch {
                case e: DBException => e.rethrow(s"Failed to update storable: $toJson")
            }
        }
        update.isDefined
    }
    
    /**
     * Updates certain properties to the database
     * @return whether any update was performed
     */
    def updateProperties(name1: String, more: String*)(implicit connection: Connection): Boolean = 
            updateProperties(name1 +: more)
    
    /**
     * Creates an update statement that updates only the specified properties
     * @param propertyNames the names of the properties that will be included in the update segment
     */
    def updateStatementForProperties(propertyNames: Iterable[String]) = {
        def updatedProperties = valueProperties
            .filter { case (name, _) => propertyNames.exists(name.equalsIgnoreCase) }
        Update(table, Model(updatedProperties))
    }
    /**
     * Creates an update statement that updates only the specified properties
     */
    def updateStatementForProperties(name1: String, more: String*): SqlSegment =
        updateStatementForProperties(name1 +: more)
    
    /**
     * Pushes storable data to the database, but will always insert the instance as a new row 
     * instead of updating an existing row.
     * @return The generated index, if an insertion was made and one was generated or provided.
      * @throws IllegalStateException If inserting an item without index into a non-auto-increment-indexing table
      *                               that contains a primary key
     */
    @throws[IllegalStateException]("No primary key specified when required")
    def insert()(implicit connection: Connection) = {
        try {
            table.primaryColumn match {
                // Case: Table uses indexing
                case Some(primary) =>
                    // Case: Auto-Increment indexing used => Will never insert a custom index
                    if (table.usesAutoIncrement)
                        Insert(table, toModel - primary.name).generatedKeys.headOption
                            .getOrElse(Value.emptyWithType(primary.dataType))
                    // Case: Indexing required => Throws if index hasn't been specified
                    else {
                        index.notEmpty match {
                            case Some(i) =>
                                Insert(table, toModel)
                                i
                            case None =>
                                throw new IllegalStateException(s"Attempting to insert storable $toJson to table ${
                                    table.name} without specifying the primary key")
                        }
                    }
                // Case: No indexing used. Simply inserts a new row.
                case None =>
                    Insert(table, toModel)
                    Value.empty
            }
        }
        catch {
            case e: DBException => e.rethrow(s"Failed to insert storable: $toJson. Message: ${e.getMessage}")
        }
    }
    
    /**
     * Deletes this storable instance from the database. If the storable has no index, this 
     * method does nothing
     */
    def delete()(implicit connection: Connection) =
        indexCondition.map { Delete(table) + Where(_) }.foreach { _.execute() }
    
    /**
      * Searches for a row by using this storable instance as the search condition
      * @param factory A factory for producing the result object
      * @param connection A database connection (implicit)
      * @tparam B Type of resulting object
      * @return An object from the database, if one could be found
      */
    def search[B](factory: FromRowFactory[B])(implicit connection: Connection) = factory.find(toCondition)
    
    /**
      * Searches for multiple rows using this storable instance as the search condition
      * @param factory A factory for producing the result objects
      * @param connection A database connection (implicit)
      * @tparam B Type of resulting object
      * @return Objects from the database matching this condition
      */
    def searchMany[B](factory: FromRowFactory[B])(implicit connection: Connection) =
        factory.findMany(toCondition)
    
    // NB: Throws if there were no specified attributes
    private def makeCondition(makePart: (Column, Value) => Condition, combineOperator: BasicCombineOperator = And) =
    {
        val model = toModel
        // Converts each defined (non-empty) attribute + column to a condition
        val conditions = table.columns.flatMap { c => model.existing(c.name).map { _.value }.filter {
            _.isDefined }.map { makePart(c, _) } }
        // Merges the specified conditions using AND
        conditions.head.combineWith(conditions.drop(1), combineOperator)
    }
}