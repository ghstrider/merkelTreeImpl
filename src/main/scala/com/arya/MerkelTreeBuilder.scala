package com.arya

import java.math.BigInteger
import java.nio.charset.StandardCharsets
import java.security.MessageDigest

class MerkelTreeBuilder(list: List[String]) {
  def init(): MerkelTree = {
    val sz = list.size
    
    // Handle empty list
    if (sz == 0) {
      throw new IllegalArgumentException("Cannot create Merkle tree from empty list")
    }
    
    // Calculate the next power of 2
    var targetSize = 1
    while (targetSize < sz) {
      targetSize *= 2
    }
    
    // Pad the list to make it a power of 2
    val paddedList = if (targetSize == sz) {
      list
    } else {
      list ++ List.fill(targetSize - sz)(list.last)
    }

    val md = MessageDigest.getInstance("SHA-256")

    def merkelTreeBuilder(ll: List[String]): MerkelTree = {
      if (ll.size == 1) {
        Leaf(ll.head.getBytes.toList, this.getHash(ll.head, md))
      } else if (ll.size == 2) {
        val leftLeaf = Leaf(ll.head.getBytes.toList, this.getHash(ll.head, md))
        val rightLeaf = Leaf(ll.tail.head.getBytes.toList, this.getHash(ll.tail.head, md))

        Node(leftLeaf,  leftLeaf.bytes ++ rightLeaf.bytes, getHash(leftLeaf.sha256 + rightLeaf.sha256, md), rightLeaf)
      } else {
        val half = ll.size / 2
        val leftSide: MerkelTree = merkelTreeBuilder(ll.take(half))
        val rightSide: MerkelTree = merkelTreeBuilder(ll.takeRight(half))
        
        val leftHash = leftSide match {
          case Leaf(_, hash) => hash
          case Node(_, _, hash, _) => hash
        }
        val rightHash = rightSide match {
          case Leaf(_, hash) => hash
          case Node(_, _, hash, _) => hash
        }
        val leftBytes = leftSide match {
          case Leaf(bytes, _) => bytes
          case Node(_, bytes, _, _) => bytes
        }
        val rightBytes = rightSide match {
          case Leaf(bytes, _) => bytes
          case Node(_, bytes, _, _) => bytes
        }
        
        Node(leftSide, leftBytes ++ rightBytes, getHash(leftHash + rightHash, md), rightSide)
      }
    }

    merkelTreeBuilder(paddedList)
  }


  def getHash(text: String, md: MessageDigest): String = {
    md.reset()
    md.update(text.getBytes(StandardCharsets.UTF_8))
    val digest = md.digest()

    String.format("%064x", new BigInteger(1, digest))
  }
}
