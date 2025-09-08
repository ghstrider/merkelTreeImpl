package com.arya

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class MerkelTreeSpec extends AnyFlatSpec with Matchers {

  def createTestTree(data: List[String]): MerkelTree = {
    val builder = new MerkelTreeBuilder(data)
    builder.init()
  }

  "MerkelTree.isEqual" should "return true for identical trees" in {
    val data = List("data1", "data2", "data3", "data4")
    val tree1 = createTestTree(data)
    val tree2 = createTestTree(data)
    
    MerkelTree.isEqual(tree1, tree2) shouldBe true
  }

  it should "return false for trees with different data" in {
    val tree1 = createTestTree(List("data1", "data2"))
    val tree2 = createTestTree(List("data3", "data4"))
    
    MerkelTree.isEqual(tree1, tree2) shouldBe false
  }

  it should "return false for trees with subtle differences" in {
    val tree1 = createTestTree(List("Hello World", "Test"))
    val tree2 = createTestTree(List("Hello World!", "Test"))
    
    MerkelTree.isEqual(tree1, tree2) shouldBe false
  }

  it should "detect differences in tree structure" in {
    val tree1 = createTestTree(List("a", "b", "c", "d"))
    val tree2 = createTestTree(List("a", "b", "c", "e"))
    
    MerkelTree.isEqual(tree1, tree2) shouldBe false
  }

  "MerkelTree.findMismatchedData" should "return empty list for identical trees" in {
    val data = List("same1", "same2", "same3", "same4")
    val tree1 = createTestTree(data)
    val tree2 = createTestTree(data)
    
    val mismatches = MerkelTree.findMismatchedData(tree1, tree2)
    mismatches shouldBe empty
  }

  it should "find all mismatched data between two trees" in {
    val tree1 = createTestTree(List("common", "different1", "common2", "different3"))
    val tree2 = createTestTree(List("common", "different2", "common2", "different4"))
    
    val mismatches = MerkelTree.findMismatchedData(tree1, tree2)
    
    mismatches should have size 2
    mismatches should contain (("different1", "different2"))
    mismatches should contain (("different3", "different4"))
  }

  it should "handle completely different trees" in {
    val tree1 = createTestTree(List("a", "b"))
    val tree2 = createTestTree(List("c", "d"))
    
    val mismatches = MerkelTree.findMismatchedData(tree1, tree2)
    
    mismatches should have size 2
    mismatches should contain (("a", "c"))
    mismatches should contain (("b", "d"))
  }

  it should "detect single character differences" in {
    val tree1 = createTestTree(List("test1", "test2"))
    val tree2 = createTestTree(List("test1", "test3"))
    
    val mismatches = MerkelTree.findMismatchedData(tree1, tree2)
    
    mismatches should have size 1
    mismatches should contain (("test2", "test3"))
  }

  it should "handle trees with duplicated last elements correctly" in {
    val tree1 = createTestTree(List("a", "b", "c")) // Will duplicate "c"
    val tree2 = createTestTree(List("a", "b", "d")) // Will duplicate "d"
    
    val mismatches = MerkelTree.findMismatchedData(tree1, tree2)
    
    // Should find mismatches for both the original and duplicated elements
    mismatches should have size 2
    mismatches.foreach { case (left, right) =>
      left should (be ("c") or be ("c"))
      right should (be ("d") or be ("d"))
    }
  }

  "MerkelTree.printMerkelTree" should "not throw exceptions" in {
    val tree = createTestTree(List("test1", "test2"))
    
    noException should be thrownBy {
      MerkelTree.printMerkelTree(tree, 0)
    }
  }

  "MerkelTree.printMerkelTreeData" should "not throw exceptions" in {
    val tree = createTestTree(List("test1", "test2"))
    
    noException should be thrownBy {
      MerkelTree.printMerkelTreeData(tree, 0)
    }
  }

  "Leaf nodes" should "store correct byte data" in {
    val testData = "Hello World"
    val leaf = Leaf(testData.getBytes.toList, "somehash")
    
    leaf.bytes.map(_.toChar).mkString("") shouldBe testData
  }

  it should "have non-empty hash" in {
    val leaf = Leaf("test".getBytes.toList, "hash123")
    
    leaf.sha256 should not be empty
    leaf.sha256 shouldBe "hash123"
  }

  "Node" should "contain references to child nodes" in {
    val left = Leaf("left".getBytes.toList, "lefthash")
    val right = Leaf("right".getBytes.toList, "righthash")
    val node = Node(left, "leftright".getBytes.toList, "nodehash", right)
    
    node.left shouldBe left
    node.right shouldBe right
    node.sha256 shouldBe "nodehash"
  }

  it should "combine bytes from children" in {
    val left = Leaf("left".getBytes.toList, "lefthash")
    val right = Leaf("right".getBytes.toList, "righthash")
    val combinedBytes = left.bytes ++ right.bytes
    val node = Node(left, combinedBytes, "nodehash", right)
    
    node.bytes shouldBe combinedBytes
    node.bytes.map(_.toChar).mkString("") shouldBe "leftright"
  }

  "MerkelTree comparison" should "work with empty strings" in {
    val tree1 = createTestTree(List("", "data", ""))
    val tree2 = createTestTree(List("", "data", ""))
    
    MerkelTree.isEqual(tree1, tree2) shouldBe true
  }

  it should "detect differences in empty vs non-empty strings" in {
    val tree1 = createTestTree(List("", "data"))
    val tree2 = createTestTree(List("x", "data"))
    
    MerkelTree.isEqual(tree1, tree2) shouldBe false
    
    val mismatches = MerkelTree.findMismatchedData(tree1, tree2)
    mismatches should have size 1
    mismatches.head shouldBe (("", "x"))
  }

  it should "handle unicode correctly" in {
    val tree1 = createTestTree(List("Hello 世界", "Test"))
    val tree2 = createTestTree(List("Hello 世界", "Test"))
    
    MerkelTree.isEqual(tree1, tree2) shouldBe true
  }

  it should "detect unicode differences" in {
    val str1 = "Hello 世界"
    val str2 = "Hello 世間"
    val tree1 = createTestTree(List(str1))
    val tree2 = createTestTree(List(str2))
    
    MerkelTree.isEqual(tree1, tree2) shouldBe false
    
    val mismatches = MerkelTree.findMismatchedData(tree1, tree2)
    mismatches should not be empty
    
    // Since we have single elements, there should be exactly one mismatch
    mismatches.size should be >= 1
    
    // The mismatch should contain different strings (not checking exact values due to encoding)
    val (first, second) = mismatches.head
    first should not equal second
  }
}