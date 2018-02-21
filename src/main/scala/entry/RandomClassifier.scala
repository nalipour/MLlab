package mllab

import scala.collection.mutable.ListBuffer


class RandomClassifier() {

  def train(X: List[List[Float]], y: List[Int]): Unit = {
    require(X.length == y.length, "both arguments must have the same length")
  }

  def predict(X: List[List[Float]]): List[Int] = {
    var result = new ListBuffer[Float]()
    for (instance <- X){
      val prediction = if (Math.random < 0.5) 0 else 1
      result += prediction
    }
    result.toList.map(_.toInt)
  }

}
