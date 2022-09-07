import com.arya.{MerkelTree, MerkelTreeBuilder}

object Main {
  def main(args: Array[String]): Unit = {
    val list  = "This sentence is equal\nThis sentence is ok but on byte is different --> 1\nAgain this is correct\nDifferent sentence".split("\n").toList
    val list2 = "This sentence is equal\nThis sentence is ok but on byte is different --> 2\nAgain this is correct\nNothing is common".split("\n").toList

    val builder1 = new MerkelTreeBuilder(list)
    val tree1: MerkelTree = builder1.init()

    val builder2 = new MerkelTreeBuilder(list2)
    val tree2: MerkelTree = builder2.init()

    MerkelTree.findMismatchedData(tree1, tree2).foreach(x => println(x._1 +"\n"+x._2+"\n\n"))
  }
}