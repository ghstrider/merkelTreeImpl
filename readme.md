# Merkle Tree Implementation in Scala

A robust implementation of Merkle Trees (also known as hash trees) in Scala, providing efficient data structure verification and comparison capabilities using SHA-256 hashing.

## Overview

A Merkle tree is a binary tree structure where every leaf node contains a hash of a data block, and every non-leaf node contains the cryptographic hash of its children's hashes. This implementation allows you to:

- Build Merkle trees from string data
- Compare two Merkle trees to identify differences
- Verify data integrity efficiently
- Find mismatched data between two trees

## Features

- **SHA-256 Hashing**: Uses secure SHA-256 algorithm for generating hashes
- **Tree Construction**: Builds balanced binary Merkle trees from input data
- **Tree Comparison**: Efficiently compares two trees and identifies differences
- **Data Verification**: Quickly verify if data has been tampered with
- **Automatic Balancing**: Handles odd-numbered inputs by duplicating the last element

## Project Structure

```
merkelTreeImpl/
├── build.sbt
├── src/
│   └── main/
│       └── scala/
│           ├── Main.scala                    # Example usage and demonstration
│           └── com/
│               └── arya/
│                   ├── MerkelTree.scala      # Core tree data structures and operations
│                   └── MerkelTreeBuilder.scala # Tree construction logic
```

## Requirements

- Scala 2.13.8
- SBT (Scala Build Tool)
- Java 8 or higher

## Installation

1. Clone the repository:
```bash
git clone <repository-url>
cd merkelTreeImpl
```

2. Build the project:
```bash
sbt compile
```

## Usage

### Basic Example

```scala
import com.arya.{MerkelTree, MerkelTreeBuilder}

// Create data for the first tree
val data1 = List(
  "This sentence is equal",
  "This sentence has a difference",
  "Again this is correct",
  "Different sentence"
)

// Create data for the second tree
val data2 = List(
  "This sentence is equal",
  "This sentence has another difference",
  "Again this is correct",
  "Nothing is common"
)

// Build the first Merkle tree
val builder1 = new MerkelTreeBuilder(data1)
val tree1: MerkelTree = builder1.init()

// Build the second Merkle tree
val builder2 = new MerkelTreeBuilder(data2)
val tree2: MerkelTree = builder2.init()

// Find differences between the trees
val differences = MerkelTree.findMismatchedData(tree1, tree2)
differences.foreach { case (data1, data2) =>
  println(s"Tree 1: $data1")
  println(s"Tree 2: $data2")
  println()
}
```

### Running the Example

```bash
sbt run
```

This will execute the `Main.scala` file which demonstrates comparing two Merkle trees and identifying the differences between them.

## API Reference

### MerkelTreeBuilder

```scala
class MerkelTreeBuilder(list: List[String])
```

- `init(): MerkelTree` - Builds and returns a Merkle tree from the input list

### MerkelTree

The base trait with two implementations:
- `Leaf(bytes: List[Byte], sha256: String)` - Represents a leaf node
- `Node(left: MerkelTree, bytes: List[Byte], sha256: String, right: MerkelTree)` - Represents an internal node

#### Companion Object Methods

- `isEqual(t1: MerkelTree, t2: MerkelTree): Boolean` - Checks if two trees are identical
- `findMismatchedData(t1: MerkelTree, t2: MerkelTree): List[(String, String)]` - Returns pairs of differing data
- `printMerkelTree(t: MerkelTree, depth: Int): Unit` - Prints tree hashes
- `printMerkelTreeData(t: MerkelTree, depth: Int): Unit` - Prints tree data

## How It Works

1. **Tree Construction**: 
   - Input strings are converted to byte arrays
   - Each string is hashed using SHA-256 to create leaf nodes
   - Pairs of nodes are combined, and their concatenated hashes are hashed again to create parent nodes
   - This process continues until a single root node is reached

2. **Tree Comparison**:
   - Trees are traversed recursively
   - Hash values are compared at each level
   - When mismatches are found, the algorithm drills down to identify the specific differing data

3. **Balancing**:
   - If the input has an odd number of elements, the last element is duplicated to maintain a balanced binary tree

## Use Cases

- **Data Integrity Verification**: Verify large datasets haven't been tampered with
- **Distributed Systems**: Synchronize data across multiple nodes efficiently
- **Blockchain Applications**: Core component in blockchain technology for transaction verification
- **File Systems**: Detect changes in file systems or version control
- **Database Replication**: Identify differences between database replicas

## Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

## License

This project is open source and available under the [MIT License](LICENSE).