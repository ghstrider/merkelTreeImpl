package com.arya

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import java.security.MessageDigest

class MerkelTreeBuilderSpec extends AnyFlatSpec with Matchers {

  "MerkelTreeBuilder" should "create a valid Merkle tree from a list of strings" in {
    val data = List("data1", "data2", "data3", "data4")
    val builder = new MerkelTreeBuilder(data)
    val tree = builder.init()
    
    tree should not be null
    tree shouldBe a [Node]
  }

  it should "handle a list with two elements" in {
    val data = List("first", "second")
    val builder = new MerkelTreeBuilder(data)
    val tree = builder.init()
    
    tree match {
      case Node(left, _, _, right) =>
        left shouldBe a [Leaf]
        right shouldBe a [Leaf]
      case _ => fail("Expected a Node with two Leaf children")
    }
  }

  it should "handle a list with odd number of elements by duplicating the last element" in {
    val data = List("first", "second", "third")
    val builder = new MerkelTreeBuilder(data)
    val tree = builder.init()
    
    tree should not be null
    tree shouldBe a [Node]
    
    // The tree should be balanced (power of 2 leaves)
    def countLeaves(t: MerkelTree): Int = t match {
      case Leaf(_, _) => 1
      case Node(left, _, _, right) => countLeaves(left) + countLeaves(right)
    }
    
    val leafCount = countLeaves(tree)
    leafCount shouldBe 4 // 3 elements + 1 duplicate
  }

  it should "handle a single element list" in {
    val data = List("single")
    val builder = new MerkelTreeBuilder(data)
    val tree = builder.init()
    
    tree match {
      case Leaf(bytes, _) =>
        // Single element becomes a single leaf
        bytes.map(_.toChar).mkString("") shouldBe "single"
      case _ => fail("Expected a single Leaf for single element")
    }
  }

  it should "generate consistent SHA-256 hashes" in {
    val data = List("test")
    val builder1 = new MerkelTreeBuilder(data)
    val builder2 = new MerkelTreeBuilder(data)
    
    val tree1 = builder1.init()
    val tree2 = builder2.init()
    
    def getRootHash(t: MerkelTree): String = t match {
      case Leaf(_, hash) => hash
      case Node(_, _, hash, _) => hash
    }
    
    getRootHash(tree1) shouldBe getRootHash(tree2)
  }

  it should "create different hashes for different data" in {
    val data1 = List("data1", "data2")
    val data2 = List("data3", "data4")
    
    val builder1 = new MerkelTreeBuilder(data1)
    val builder2 = new MerkelTreeBuilder(data2)
    
    val tree1 = builder1.init()
    val tree2 = builder2.init()
    
    def getRootHash(t: MerkelTree): String = t match {
      case Leaf(_, hash) => hash
      case Node(_, _, hash, _) => hash
    }
    
    getRootHash(tree1) should not be getRootHash(tree2)
  }

  it should "correctly compute hash using SHA-256" in {
    val testString = "test"
    val builder = new MerkelTreeBuilder(List(testString))
    val md = MessageDigest.getInstance("SHA-256")
    
    val expectedHash = builder.getHash(testString, md)
    
    // Verify the hash format is 64 hexadecimal characters (256 bits)
    expectedHash should have length 64
    expectedHash should fullyMatch regex "[0-9a-f]{64}"
  }

  it should "handle empty strings in the list" in {
    val data = List("", "data", "")
    val builder = new MerkelTreeBuilder(data)
    val tree = builder.init()
    
    tree should not be null
    tree shouldBe a [Node]
  }

  it should "handle special characters and unicode" in {
    val data = List("Hello 世界", "Test@#$%", "Émoji 😀", "Line\nBreak")
    val builder = new MerkelTreeBuilder(data)
    val tree = builder.init()
    
    tree should not be null
    tree shouldBe a [Node]
  }

  it should "build a balanced tree for power-of-2 elements" in {
    val data = List("1", "2", "3", "4", "5", "6", "7", "8")
    val builder = new MerkelTreeBuilder(data)
    val tree = builder.init()
    
    def getDepth(t: MerkelTree): Int = t match {
      case Leaf(_, _) => 0
      case Node(left, _, _, right) => 1 + Math.max(getDepth(left), getDepth(right))
    }
    
    def isBalanced(t: MerkelTree): Boolean = t match {
      case Leaf(_, _) => true
      case Node(left, _, _, right) =>
        val leftDepth = getDepth(left)
        val rightDepth = getDepth(right)
        Math.abs(leftDepth - rightDepth) <= 1 && isBalanced(left) && isBalanced(right)
    }
    
    isBalanced(tree) shouldBe true
    getDepth(tree) shouldBe 3 // log2(8) = 3
  }
}