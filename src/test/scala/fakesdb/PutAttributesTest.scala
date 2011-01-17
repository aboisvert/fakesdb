package fakesdb

import org.junit._
import org.junit.Assert._
import scala.collection.mutable._
import scala.collection.JavaConversions._
import com.xerox.amazonws.sdb._

class PutAttributesTest extends AbstractFakeSdbTest {

  private val nameOf1025 = "a" * 1025

  @Before
  def createDomain(): Unit = {
    sdb.createDomain("domaina")
  }

  @Test
  def testPutOne(): Unit = {
    add(domaina, "itema", "a" -> "1")
    val attrs = domaina.getItem("itema").getAttributes
    assertEquals(1, attrs.size)
    assertEquals("a", attrs.get(0).getName)
    assertEquals("1", attrs.get(0).getValue)
  }

  @Test
  def testPutMultipleValues(): Unit = {
    add(domaina, "itema", "a" -> "1", "a" -> "2")
    val attrs = domaina.getItem("itema").getAttributes
    assertEquals(2, attrs.size)
    assertEquals("a", attrs.get(0).getName)
    assertEquals("1", attrs.get(0).getValue)
    assertEquals("a", attrs.get(1).getName)
    assertEquals("2", attrs.get(1).getValue)
  }

  @Test
  def testPutMultipleValuesWithSameValue(): Unit = {
    add(domaina, "itema", "a" -> "1", "a" -> "1")
    val attrs = domaina.getItem("itema").getAttributes
    assertEquals(1, attrs.size)
    assertEquals("a", attrs.get(0).getName)
    assertEquals("1", attrs.get(0).getValue)
  }

  @Test
  def testLimitInTwoRequests(): Unit = {
    add(domaina, "itema", "a" -> "1", "b" -> "1")
    assertFails("NumberItemAttributesExceeded", "Too many attributes in this item", {
      addLots("itema", 255)
    })
    val attrs = domaina.getItem("itema").getAttributes
    assertEquals(true, attrs find (_.getName == "attr1") isDefined)
    assertEquals(true, attrs find (_.getName == "attr254") isDefined)
    assertEquals(false, attrs find (_.getName == "attr255") isDefined)
  }

  @Test
  def testLimitInTwoRequestsWithOverlappingAttributesIsOkay(): Unit = {
    add(domaina, "itema", "attr256" -> "value256") // value255 matches our new value
    addLots("itema", 256)
    val attrs = domaina.getItem("itema").getAttributes
    assertEquals(256, attrs.size)
    assertEquals(true, attrs find (_.getName == "attr1") isDefined)
    assertEquals(true, attrs find (_.getName == "attr256") isDefined)
  }

  @Test
  def testLimitInTwoRequestsWithNonOverlappingAttributeValuesFails(): Unit = {
    add(domaina, "itema", "attr256" -> "valueFoo") // valueFoo does not match our new value
    assertFails("NumberItemAttributesExceeded", "Too many attributes in this item", {
      addLots("itema", 256)
    })
    val attrs = domaina.getItem("itema").getAttributes
    assertEquals(256, attrs.size)
    assertEquals(true, attrs find ((a) => a.getName == "attr1") isDefined)
    assertEquals(true, attrs find ((a) => a.getName == "attr256" && a.getValue == "value256") isEmpty)
  }

  @Test
  def testLimitInOneRequest(): Unit = {
    assertFails("NumberItemAttributesExceeded", "Too many attributes in this item", {
      addLots("itema", 257)
    })
    val attrs = domaina.getItem("itema").getAttributes
    assertEquals(true, attrs find (_.getName == "attr257") isEmpty)
  }

  @Test
  def testFailEmptyAttributeName(): Unit = {
    assertFails("InvalidParameterValue", "Value () for parameter Name is invalid. The empty string is an illegal attribute name", {
      add(domaina, "itema", "" -> "1")
    })
  }

  @Test
  def testConditionalPutSucceeds(): Unit = {
    add(domaina, "itema", "a" -> "1")
    add(domaina, "itema", hasValue("a", "1"), "b" -> "1")
    val attrs = domaina.getItem("itema").getAttributes
    assertEquals(2, attrs.size)
    assertEquals("a", attrs.get(0).getName)
    assertEquals("1", attrs.get(0).getValue)
    assertEquals("b", attrs.get(1).getName)
    assertEquals("1", attrs.get(1).getValue)
  }

  @Test
  def testConditionalPutFailsWithWrongValue(): Unit = {
    add(domaina, "itema", "a" -> "1")
    assertFails("ConditionalCheckFailed", "Attribute (a) value is (List(1)) but was expected (2)", {
      add(domaina, "itema", hasValue("a", "2"), "b" -> "1")
    })
  }

  @Test
  def testConditionalPutDoesNotExist(): Unit = {
    add(domaina, "itema", "a" -> "1")
    assertFails("ConditionalCheckFailed", "Attribute (a) value exists", {
      add(domaina, "itema", doesNotExist("a"), "b" -> "1")
    })
  }

  @Test
  def testConditionalPutFailsAgainstMutipleValues(): Unit = {
    add(domaina, "itema", "a" -> "1", "a" -> "2")
    assertFails("ConditionalCheckFailed", "Attribute (a) value is (List(1, 2)) but was expected (1)", {
      add(domaina, "itema", hasValue("a", "1"), "b" -> "1")
    })
  }

  @Test
  def testConditionalPutFailsAgainstInvalidAttribute(): Unit = {
    add(domaina, "itema", "a" -> "1")
    assertFails("AttributeDoesNotExist", "Attribute (c) does not exist", {
      add(domaina, "itema", hasValue("c", "1"), "b" -> "1")
    })
  }

  @Test
  def testTooLongItemName(): Unit = {
    assertFails("InvalidParameterValue", "Value (\"%s\") for parameter Name is invalid. Value exceeds maximum length of 1024.".format(nameOf1025), {
      add(domaina, nameOf1025, "a" -> "1")
    })
  }

  @Test
  def testTooLongAttributeName(): Unit = {
    assertFails("InvalidParameterValue", "Value (\"%s\") for parameter Name is invalid. Value exceeds maximum length of 1024.".format(nameOf1025), {
      add(domaina, "i", nameOf1025 -> "1")
    })
  }

  @Test
  def testTooLongValue(): Unit = {
    assertFails("InvalidParameterValue", "Value (\"%s\") for parameter Value is invalid. Value exceeds maximum length of 1024.".format(nameOf1025), {
      add(domaina, "i", "a" -> nameOf1025)
    })
  }

  private def addLots(itemName: String, number: Int): Unit = {
    val list = new java.util.ArrayList[ItemAttribute]()
    for (i <- 1.to(number)) {
      list.add(new ItemAttribute("attr" + i, "value" + i, false))
    }
    domaina getItem itemName putAttributes(list)
  }

}
