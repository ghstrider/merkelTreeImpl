package com.arya

import java.math.BigInteger
import java.nio.charset.StandardCharsets
import java.security.MessageDigest

class MerkelTreeBuilder(list: List[String]) {
  def init(): MerkelTree = {
    val sz = list.size
    val newList = if(sz % 2 != 0) {
      val ele = list(sz - 1)
      list ++ List(ele)
    } else {
      list
    }

    val md = MessageDigest.getInstance("SHA-256")

    def merkelTreeBuilder(ll: List[String]): MerkelTree = {
      if (ll.size == 2) {
        val leftLeaf = Leaf(ll.head.getBytes.toList, this.getHash(ll.head, md))
        val rightLeaf = Leaf(ll.tail.head.getBytes.toList, this.getHash(ll.tail.head, md))

        Node(leftLeaf,  leftLeaf.bytes ++ rightLeaf.bytes, getHash(leftLeaf.sha256 + rightLeaf.sha256, md), rightLeaf)
      } else {
        val half = ll.size / 2
        val leftSide: MerkelTree = merkelTreeBuilder(ll.take(half))
        val rightSide: MerkelTree = merkelTreeBuilder(ll.takeRight(half))
        Node(leftSide, leftSide.asInstanceOf[Node].bytes ++ rightSide.asInstanceOf[Node].bytes, getHash(leftSide.asInstanceOf[Node].sha256 + rightSide.asInstanceOf[Node].sha256, md), rightSide)
      }
    }

    merkelTreeBuilder(newList)
  }


  def getHash(text: String, md: MessageDigest): String = {
    md.reset()
    md.update(text.getBytes(StandardCharsets.UTF_8))
    val digest = md.digest()

    String.format("%064x", new BigInteger(1, digest))
  }
}
