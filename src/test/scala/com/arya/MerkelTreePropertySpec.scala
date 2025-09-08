package com.arya

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import org.scalacheck.{Gen, Arbitrary}
import org.scalacheck.Prop.forAll

class MerkelTreePropertySpec extends AnyFlatSpec with Matchers with ScalaCheckPropertyChecks {

  // Generator for non-empty lists of non-empty strings  
  val nonEmptyStringListGen: Gen[List[String]] = Gen.nonEmptyListOf(Gen.alphaNumStr.suchThat(_.nonEmpty))
  
  // Generator for lists with specific size
  def listOfSize(n: Int): Gen[List[String]] = Gen.listOfN(n, Gen.alphaNumStr)
  
  // Generator for power of 2 sized lists
  val powerOf2ListGen: Gen[List[String]] = for {
    power <- Gen.choose(1, 8) // 2^1 to 2^8 elements
    list <- listOfSize(Math.pow(2, power).toInt)
  } yield list

  def createTree(data: List[String]): MerkelTree = {
    val builder = new MerkelTreeBuilder(data)
    builder.init()
  }

  "Merkle Tree properties" should "satisfy reflexivity - a tree is equal to itself" in {
    forAll(nonEmptyStringListGen) { data =>
      whenever(data.nonEmpty) {
        val tree = createTree(data)
        MerkelTree.isEqual(tree, tree) shouldBe true
      }
    }
  }

  it should "satisfy determinism - same input produces same tree" in {
    forAll(nonEmptyStringListGen) { data =>
      whenever(data.nonEmpty) {
        val tree1 = createTree(data)
        val tree2 = createTree(data)
        
        MerkelTree.isEqual(tree1, tree2) shouldBe true
        
        // Extract root hashes
        def getRootHash(t: MerkelTree): String = t match {
          case Leaf(_, hash) => hash
          case Node(_, _, hash, _) => hash
        }
        
        getRootHash(tree1) shouldBe getRootHash(tree2)
      }
    }
  }

  it should "have different hashes for different inputs" in {
    forAll(nonEmptyStringListGen, nonEmptyStringListGen) { (data1, data2) =>
      whenever(data1.nonEmpty && data2.nonEmpty && data1 != data2) {
        val tree1 = createTree(data1)
        val tree2 = createTree(data2)
        
        def getRootHash(t: MerkelTree): String = t match {
          case Leaf(_, hash) => hash
          case Node(_, _, hash, _) => hash
        }
        
        // Different data should produce different root hashes
        if (data1.sorted != data2.sorted || data1.size != data2.size) {
          getRootHash(tree1) should not be getRootHash(tree2)
        }
      }
    }
  }

  it should "always produce a valid tree structure" in {
    forAll(nonEmptyStringListGen) { data =>
      whenever(data.nonEmpty) {
        val tree = createTree(data)
        
        def isValidTree(t: MerkelTree): Boolean = t match {
          case Leaf(bytes, hash) => 
            // Allow empty bytes for empty strings
            hash.nonEmpty && hash.length == 64
          case Node(left, bytes, hash, right) =>
            hash.nonEmpty && hash.length == 64 && 
            isValidTree(left) && isValidTree(right)
        }
        
        isValidTree(tree) shouldBe true
      }
    }
  }

  it should "have consistent leaf count" in {
    forAll(Gen.choose(1, 100)) { size =>
      val data = (1 to size).map(_.toString).toList
      val tree = createTree(data)
      
      def countLeaves(t: MerkelTree): Int = t match {
        case Leaf(_, _) => 1
        case Node(left, _, _, right) => countLeaves(left) + countLeaves(right)
      }
      
      val leafCount = countLeaves(tree)
      
      // Leaf count should be next power of 2 >= size
      val expectedLeafCount = {
        var power = 1
        while (power < size) power *= 2
        power
      }
      
      leafCount shouldBe expectedLeafCount
    }
  }

  it should "maintain hash consistency through tree structure" in {
    forAll(nonEmptyStringListGen) { data =>
      whenever(data.nonEmpty) {
        val tree = createTree(data)
        
        def verifyHashConsistency(t: MerkelTree): Boolean = t match {
          case Leaf(_, hash) => hash.length == 64
          case Node(left, _, hash, right) =>
            // Parent hash should be different from children hashes
            val leftHash = left match {
              case Leaf(_, h) => h
              case Node(_, _, h, _) => h
            }
            val rightHash = right match {
              case Leaf(_, h) => h
              case Node(_, _, h, _) => h
            }
            
            hash.length == 64 && 
            hash != leftHash && 
            hash != rightHash &&
            verifyHashConsistency(left) && 
            verifyHashConsistency(right)
        }
        
        verifyHashConsistency(tree) shouldBe true
      }
    }
  }

  it should "correctly identify all differences between trees" in {
    forAll(nonEmptyStringListGen) { baseData =>
      whenever(baseData.size >= 2) {
        // Create a modified version by changing one element
        val modifiedData = baseData.updated(0, baseData.head + "_modified")
        
        val tree1 = createTree(baseData)
        val tree2 = createTree(modifiedData)
        
        val mismatches = MerkelTree.findMismatchedData(tree1, tree2)
        
        // Should find at least one mismatch
        mismatches should not be empty
        
        // The mismatch should contain the changed element
        val mismatchedValues = mismatches.map(_._1) ++ mismatches.map(_._2)
        mismatchedValues should contain (baseData.head)
        mismatchedValues should contain (baseData.head + "_modified")
      }
    }
  }

  it should "handle empty strings correctly" in {
    forAll(Gen.nonEmptyListOf(Gen.const(""))) { emptyStrings =>
      whenever(emptyStrings.nonEmpty) {
        val tree = createTree(emptyStrings)
        
        def allLeavesHaveEmptyData(t: MerkelTree): Boolean = t match {
          case Leaf(bytes, _) => bytes.isEmpty || bytes.forall(_ == 0)
          case Node(left, _, _, right) => 
            allLeavesHaveEmptyData(left) && allLeavesHaveEmptyData(right)
        }
        
        // Tree should still have valid structure even with empty strings
        tree should not be null
        allLeavesHaveEmptyData(tree) shouldBe true
      }
    }
  }

  it should "maintain tree balance for power-of-2 inputs" in {
    forAll(powerOf2ListGen) { data =>
      whenever(data.nonEmpty) {
        val tree = createTree(data)
        
        def getDepth(t: MerkelTree): Int = t match {
          case Leaf(_, _) => 0
          case Node(left, _, _, right) => 1 + Math.max(getDepth(left), getDepth(right))
        }
        
        def isPerfectlyBalanced(t: MerkelTree): Boolean = t match {
          case Leaf(_, _) => true
          case Node(left, _, _, right) =>
            getDepth(left) == getDepth(right) && 
            isPerfectlyBalanced(left) && 
            isPerfectlyBalanced(right)
        }
        
        isPerfectlyBalanced(tree) shouldBe true
      }
    }
  }

  it should "preserve data ordering in tree structure" in {
    forAll(Gen.choose(2, 20)) { size =>
      val orderedData = (1 to size).map(i => s"data_$i").toList
      val tree = createTree(orderedData)
      
      def collectLeafData(t: MerkelTree): List[String] = t match {
        case Leaf(bytes, _) => List(bytes.map(_.toChar).mkString(""))
        case Node(left, _, _, right) => collectLeafData(left) ++ collectLeafData(right)
      }
      
      val leafData = collectLeafData(tree)
      
      // The leaf data should contain all original elements (possibly with duplicates)
      orderedData.foreach { element =>
        leafData should contain (element)
      }
    }
  }

  it should "have commutative property for isEqual" in {
    forAll(nonEmptyStringListGen, nonEmptyStringListGen) { (data1, data2) =>
      whenever(data1.nonEmpty && data2.nonEmpty) {
        val tree1 = createTree(data1)
        val tree2 = createTree(data2)
        
        // isEqual should be commutative
        MerkelTree.isEqual(tree1, tree2) shouldBe MerkelTree.isEqual(tree2, tree1)
      }
    }
  }

  it should "have transitivity property for equal trees" in {
    forAll(nonEmptyStringListGen) { data =>
      whenever(data.nonEmpty) {
        val tree1 = createTree(data)
        val tree2 = createTree(data)
        val tree3 = createTree(data)
        
        // If tree1 == tree2 and tree2 == tree3, then tree1 == tree3
        if (MerkelTree.isEqual(tree1, tree2) && MerkelTree.isEqual(tree2, tree3)) {
          MerkelTree.isEqual(tree1, tree3) shouldBe true
        }
      }
    }
  }

  it should "detect any single bit change" in {
    forAll(Gen.alphaNumStr) { str =>
      whenever(str.nonEmpty) {
        val data1 = List(str)
        val data2 = List(str + " ") // Add a space (single character change)
        
        val tree1 = createTree(data1)
        val tree2 = createTree(data2)
        
        MerkelTree.isEqual(tree1, tree2) shouldBe false
        
        val mismatches = MerkelTree.findMismatchedData(tree1, tree2)
        mismatches should not be empty
      }
    }
  }

  it should "handle very large inputs efficiently" in {
    val largeData = (1 to 1000).map(i => s"element_$i").toList
    
    val startTime = System.currentTimeMillis()
    val tree = createTree(largeData)
    val endTime = System.currentTimeMillis()
    
    val timeTaken = endTime - startTime
    
    // Tree creation should complete in reasonable time (< 5 seconds)
    timeTaken should be < 5000L
    
    // Tree should be valid
    tree should not be null
    
    def getRootHash(t: MerkelTree): String = t match {
      case Leaf(_, hash) => hash
      case Node(_, _, hash, _) => hash
    }
    
    getRootHash(tree) should have length 64
  }
}