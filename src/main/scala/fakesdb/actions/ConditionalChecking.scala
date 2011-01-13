package fakesdb.actions

import fakesdb._

trait ConditionalChecking {

  private val expectedNamePattern = """Expected\.(\d+)\.Name""".r

  def checkConditionals(item: Item, params: Params) {
    for (condition <- discoverConditional(params)) {
      condition match {
        case (name, None) => for (f <- item.getAttributes.find(_.name == name)) throw new ConditionalCheckFailedException(condition)
        case (name, Some(value)) => item.getAttributes find (_.name == name) match {
          case None => throw new AttributeDoesNotExistException(name)
          case Some(attr) => if (attr.getValues.toList != List(value)) throw new ConditionalCheckFailedException(condition, attr.getValues.toList)
        }
      }
    }
  }

  private def discoverConditional(params: Params): Option[Tuple2[String, Option[String]]] = {
    val keys = params.keys find (k => k.startsWith("Expected") && k.endsWith("Name"))
    if (keys.isEmpty) {
      return None
    }
    if (keys.size > 1) {
      error("Only one condition may be specified")
    }
    val name = params.get(keys.head).get
    keys.head match {
      case expectedNamePattern(digit) => {
        for (v <- params.get("Expected.%s.Exists".format(digit))) {
          if (v == "false") {
            return Some((name, None))
          }
        }
        for (v <- params.get("Expected.%s.Value".format(digit))) {
          return Some((name, Some(v)))
        }
      }
    }
    None
  }

  class ConditionalCheckFailedException(message: String) extends SDBException("ConditionalCheckFailed", message, 409) {
    def this(condition: Tuple2[String, Option[String]]) = {
      this("Attribute (%s) value exists".format(condition._1))
    }

    def this(condition: Tuple2[String, Option[String]], actual: List[String]) = {
      this("Attribute (%s) value is (%s) but was expected (%s)".format(condition._1, actual, condition._2.get))
    }
  }

  class AttributeDoesNotExistException(name: String)
    extends SDBException("AttributeDoesNotExist", "Attribute (%s) does not exist".format(name), 404)

}