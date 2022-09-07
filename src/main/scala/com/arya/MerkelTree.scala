package com.arya

sealed trait MerkelTree
object MerkelTree {
  def printMerkelTree(t: MerkelTree, depth: Int): Unit = t match {
    case Leaf(_, sha256) => print(sha256 + " ")
    case Node(left, _, sha256, right) =>
      printMerkelTree(left, depth + 1)
      print(sha256 + " ")
      printMerkelTree(right, depth + 1)

  }

  def printMerkelTreeData(t: MerkelTree, depth: Int): Unit = t match {
    case Leaf(bytes, _) => print(bytes.map(_.toChar).mkString("") + " ")
    case Node(left, bytes, _, right) =>
      printMerkelTreeData(left, depth + 1)
      print(bytes.map(_.toChar).mkString("") + " ")
      printMerkelTreeData(right, depth + 1)

  }

  def isEqual(t1: MerkelTree, t2: MerkelTree): Boolean = {
    (t1, t2) match {
      case (Leaf(_, sha256_1), Leaf(_, sha256_2)) => sha256_1 == sha256_2
      case (Node(l1, _,sha256_1, r1), Node(l2, _, sha256_2, r2)) => sha256_1 == sha256_2 && isEqual(l1, l2) && isEqual(r1, r2)
    }
  }

  def findMismatchedData(t1: MerkelTree, t2: MerkelTree): List[(String, String)] = {
    def go(tr1: MerkelTree, tr2: MerkelTree, acc: List[(String, String)]): List[(String, String)] = {
      (tr1, tr2) match {
        case (Leaf(d1, sha256_1), Leaf(d2, sha256_2)) => if(sha256_1 != sha256_2) {
          (d1.map(_.toChar).mkString(""), d2.map(_.toChar).mkString("")) :: acc
        } else {
          acc
        }
        case (Node(l1, d1, sha256_1, r1), Node(l2, d2, sha256_2, r2)) => go(l1, l2, acc) ++ go(r1, r2, acc)
      }
    }

    go(t1, t2, List.empty[(String, String)])
  }
}

case class Leaf(bytes: List[Byte], sha256: String) extends MerkelTree
case class Node(left: MerkelTree, bytes: List[Byte], sha256: String, right: MerkelTree) extends MerkelTree
