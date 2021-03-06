package algorithms

import utils._


/** A class representing a single node in a decision tree
  *
  * @constructor Create a new node
  * @param index Index of the node top-left to down-right
  */
case class DecisionNode(index: Int) {
  val nodeIndex: Int = index
  /** Index of right child */
  var right: Int = (nodeIndex + 1) * 2
  /** Index of left child */
  var left: Int = (nodeIndex + 1) * 2 - 1
  /** Index of parent */
  val parent: Int = if (nodeIndex == 0) -1 else (nodeIndex - 1) / 2
  /** Is this node a right child? */
  val isRightChild: Boolean = (nodeIndex % 2 == 0)

  /** Index of feature column this node decides on */
  var featureIndex: Int = -1
  /** Threshold this node applies */
  var threshold: Double = 0
  /** Is the signal greater or less than the threshold? */
  var greater: Boolean = true
  /** Has this node been touched since initialization? */
  var filled: Boolean = false
  /** Node purity */
  var purity: Double = Double.MinValue
  /** Mean label of instances in this node */
  var mean: Double = 0
  /** Number of instances in this node */
  var nSamples: Int = 0

  override def toString(): String = {
    "Node " + nodeIndex + " " + left + "<- . ->" + right
  }
}

/** A class representing a decision tree
  *
  * @constructor Create a new decision tree
  * @param depth Depth of the tree
  */
class DecisionTree(depth: Int){

  /** Number of nodes in this tree */
  val nNodes: Int = Math.pow(2, depth).toInt - 1

  /** Weight of the tree (for boosting) */
  var weight: Double = 1.0

  /** Initializes a list of nodes
  * @param nNodes number of nodes to initialize
  * @param tree start/intermediate tree object
  * @return List of nodes
  */
  def initTree(nNodes: Int, tree: List[DecisionNode]): List[DecisionNode] =
    if (tree.length < nNodes) initTree(nNodes, new DecisionNode(tree.length)::tree)
    else tree.reverse

  /** The object holding the nodes */
  val tree = initTree(nNodes, Nil)

  /** Updates an existing node with new decision instructions, in case its purity is improved
   * @param nodeIndex The index of the node to be customized
   * @param featureIndex The index of the feature the decision is based on
   * @param threshold The threshold of the proposed decision
   * @param greater Is the signal region greater or less than the threshold?
   * @param purity The purity of the proposed split
   */
  def updateNode(nodeIndex: Int, featureIndex: Int, threshold: Double, greater: Boolean, mean: Double, nSamples: Int, purity: Double): Unit = {
    if (tree(nodeIndex).purity < purity){
      // println("Improving purity of node " + nodeIndex +
      //   " with feature " + featureIndex +
      //   (if (greater) " > " else " < ") + "%+.3f".format(threshold) +
      //   ": " + "%.3e".format(tree(nodeIndex).purity) +
      //   " -> " + "%.3e".format(purity))
      setNode(nodeIndex, featureIndex, threshold, greater, mean, nSamples, purity)
    }
  }

  /** Sets node attributes
   * @param nodeIndex The index of the node to be customized
   * @param featureIndex The index of the feature the node decides on
   * @param threshold The threshold the node's decision will apply
   * @param greater Is the signal region greater or less than the threshold?
   * @param purity The purity of the split in this node
   */
  def setNode(nodeIndex: Int, featureIndex: Int, threshold: Double, greater: Boolean, mean: Double, nSamples: Int, purity: Double=Double.MinValue): Unit = {
    if (nodeIndex > tree.length - 1) {
      println("Warning: tree not deep enough! (" + nodeIndex + " > " + (tree.length - 1) + ") Ignore node.")
    }else{
      tree(nodeIndex).featureIndex = featureIndex
      tree(nodeIndex).threshold = threshold
      tree(nodeIndex).greater = greater
      tree(nodeIndex).filled = true
      if (tree(nodeIndex).right >= nNodes) tree(nodeIndex).right = -1
      if (tree(nodeIndex).left >= nNodes) tree(nodeIndex).left = -1
      tree(nodeIndex).mean = mean
      tree(nodeIndex).nSamples = nSamples
      tree(nodeIndex).purity = purity
    }
  }

  /** Counts the nodes which have been filled by the user */
  def nFilledNodes(): Int = {
    var nFilled: Int = 0
    for (node <- tree) {
      if (node.filled) {
        nFilled += 1
      }
    }
    nFilled
  }

  override def toString(): String = {

    def printNodes(): String =
      // (for { node <- tree if (node.filled || true) } yield
      (for { node <- tree } yield
        "Node " + node.nodeIndex +
        (if (node.filled) {
          ": decisive feature " + node.featureIndex +
          (if (node.greater) " > " else " < ") + "%+.3f".format(node.threshold) +
          ", parent " + node.parent +
          ", children: " + node.left + " ^ " + node.right +
          ", purity %.3e".format(node.purity) +
          ", mean label %.3f".format(node.mean) +
          ", nSamples " + node.nSamples
        }
        else {
          ": empty"
        }) +
        "\n"
      ).mkString

    "------- Decision Tree -------\n" +
    "Tree complete with " + nFilledNodes() + " / " + nNodes + " filled nodes\n" +
    printNodes +
    "------------------------------"
  }

  /** Predicits an instance's label based on its feature vector
   * @param instance Feature list of an instance
   * @return Predicted label
   */
  def predict(instance: List[Double]): Double = {
    def walkTree(currentNodeIndex: Int, label: Double): Double = {
      if (currentNodeIndex == -1) label
      else if (!tree(currentNodeIndex).filled) label
      else {
        val thisNode = tree(currentNodeIndex)
        val greater = thisNode.greater
        val featureIndex = thisNode.featureIndex
        val threshold = thisNode.threshold
        val left = thisNode.left
        val right = thisNode.right
        val mean = thisNode.mean
        if (greater)
          if (instance(featureIndex) > threshold) walkTree(right, mean)
          else walkTree(left, mean)
        else
          if (instance(featureIndex) < threshold) walkTree(left, mean)
          else walkTree(right, mean)
      }
    }

    walkTree(0, -1)
  }

  /** Classifies an instance based on its feature vector
   * @param instance Feature list of an instance
   * @return Predicted label
   */
  def classify(instance: List[Double]): Int = {
    def walkTree(currentNodeIndex: Int, label: Int): Int = {
      if (currentNodeIndex == -1) label
      else if (!tree(currentNodeIndex).filled) label
      else {
        val thisNode = tree(currentNodeIndex)
        val greater = thisNode.greater
        val featureIndex = thisNode.featureIndex
        val threshold = thisNode.threshold
        val left = thisNode.left
        val right = thisNode.right
        if (greater)
          if (instance(featureIndex) > threshold) walkTree(right, 1)
          else walkTree(left, 0)
        else
          if (instance(featureIndex) < threshold) walkTree(left, 1)
          else walkTree(right, 0)
      }
    }

    walkTree(0, -1)
  }

  /** Returns the data (instances and labels) present at this node
   * @param nodeIndex The node index
   * @param X List of instances
   * @param y List of labels
   * @param sampleWeight Optional list of sample weights
   * @return List of instances, list of labels and list of sample weights, being a subset of the input X, y and sampleWeight
   */
  def atNode[T](nodeIndex: Int, X: List[List[Double]], y: List[T], sampleWeight: List[Double]=Nil): (List[List[Double]], List[T], List[Double]) = {

    require(X.length == y.length, "both arguments must have the same length")
    if (!sampleWeight.isEmpty)
      require(X.length == sampleWeight.length, "both arguments must have the same length")

    // determine ancestors of this node
    def walkTree(currentNode: Int, ancestors: List[Tuple2[Int, Boolean]]): List[Tuple2[Int, Boolean]] =
      if (currentNode == 0) ancestors
      else {
        val parent = tree(currentNode).parent
        val isRightChild = tree(currentNode).isRightChild
        walkTree(parent, Tuple2(parent, isRightChild)::ancestors)
      }

    val ancestors = walkTree(nodeIndex, Nil)
    // println("node " + nodeIndex + " has ancestors " + ancestors)

    // successively apply cuts in nodes
    def applyCuts[T](
      X: List[List[Double]],
      y: List[T],
      sampleWeight: List[Double],
      ancestors: List[Tuple2[Int, Boolean]]): Tuple3[List[List[Double]], List[T], List[Double]] = ancestors match {
      case Nil => Tuple3(X, y, sampleWeight)
      case ancestor::rest => {
        val thisNode = tree(ancestor._1)
        if (X.isEmpty) Tuple3(Nil, Nil, Nil)
        else if (!thisNode.filled) Tuple3(X, y, sampleWeight)
        else {
          val iFeature = thisNode.featureIndex
          val thresh = thisNode.threshold
          val goRight = ancestor._2
          val featureX: List[Double] = X.transpose.apply(iFeature)
          val goodIndices = featureX.zipWithIndex.filter(xi => (goRight && xi._1 > thresh) || (!goRight && xi._1 <= thresh)).map(_._2)
          val newXy = Trafo.iloc(X zip y, goodIndices)
          val newWeights =
            if (sampleWeight.isEmpty) Nil
            else Trafo.iloc(sampleWeight, goodIndices)
          applyCuts(newXy.map(_._1), newXy.map(_._2), newWeights, rest)
        }
      }
    }

    val survivors = applyCuts(X, y, sampleWeight, ancestors)
    // println("Node " + nodeIndex + " has " + survivors._1.length + " entries")
    survivors
  }

}
