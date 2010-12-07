package fakesdb

import org.junit._
import org.junit.Assert._

class DomainMetadataTest extends AbstractFakeSdbTest {

  @Before
  def createDomain(): Unit = {
    sdb.createDomain("domaina")
  }

  @Test
  def testFoo(): Unit = {
    add(domaina, "itema", "aa" -> "111", "bb" -> "222", "bb" -> "333")

    val result = domaina.getMetadata
    assertEquals(1, result.getItemCount)
    assertEquals(5l, result.getItemNamesSizeBytes)
    assertEquals(2, result.getAttributeNameCount)
    assertEquals(4l, result.getAttributeNamesSizeBytes)
    assertEquals(3, result.getAttributeValueCount)
    assertEquals(9l, result.getAttributeValuesSizeBytes)
  }

}
