package mllab

import classifiers._
import data._
import evaluation._
import plotting._
import regressors._


object Mllab {
  def main(args: Array[String]): Unit = {
      println("Execute MLlab!")

      println{"Train the model"}
      val trainReader = new Reader("src/test/resources/train_clf.csv", label=3, index=0)
      trainReader.loadFile()
      val X_train = trainReader.getX()
      val y_train = trainReader.getY().map(_.toInt)

      // val clf = new RandomClassifier()
      // val clf = new kNNClassifier(k=3)
      // val clf = new DecisionTreeClassifier(depth=3)
      // val clf = new PerceptronClassifier(alpha=1)
      val clf = new NeuralNetworkClassifier(alpha=0.01, activation= "tanh", layers=List(2, 10, 10, 2), regularization=0.05)
      // val clf = new SVMClassifier()
      clf.train(X_train, y_train)
      // println("Check prediction on training set")
      // clf.predict(X_train)


      println{"Apply to test set"}
      val testReader = new Reader("src/test/resources/test_clf.csv", label=3, index=0)
      testReader.loadFile()
      val X_test = testReader.getX()
      val y_test = testReader.getY().map(_.toInt)

      println("Now do prediction on test set")
      val y_pred = clf.predict(X_test)
      assert (y_pred.length == y_test.length)
      println("Predicted values:")
      // for (i <- 0 until Math.min(y_pred.length, 10)) {
      //   println("Test instance " + i + ": prediction " + y_pred(i) + " true value " + y_test(i))
      // }

      println("Evaulate of the model")
      Evaluation.matrix(y_pred, y_test)
      println("Precision: %.2f".format(Evaluation.precision(y_pred, y_test)))
      println("Recall: %.2f".format(Evaluation.recall(y_pred, y_test)))
      println("f1: %.2f".format(Evaluation.f1(y_pred, y_test)))

      println("Visualize the data")
      Plotting.plotData(X_train, y_train)
      Plotting.plotClf(X_train, y_train, clf)
      Plotting.plotGrid(X_train, clf)

      val mDiag = clf.diagnostics
      Plotting.plotCurves(
        List(mDiag.getOrElse("loss", List((0.0, 0.0))),
        mDiag.getOrElse("alpha", List((0.0, 0.0))))
      )

      // println("\n\nTry basic regressor functionality")
      //
      // val trainReader_reg = new Reader("src/test/resources/train_reg.csv", label= -1, index=0)
      // trainReader_reg.loadFile()
      // val X_train_reg = trainReader_reg.getX()
      // val y_train_reg = trainReader_reg.getY()
      //
      // val testReader_reg = new Reader("src/test/resources/test_reg.csv", label= -1, index=0)
      // testReader_reg.loadFile()
      // val X_test_reg = testReader_reg.getX()
      // val y_test_reg = testReader_reg.getY()
      //
      // println("Test feature vector: " + X_train_reg.head + " with label " + y_train_reg.head)
      //
      // // val reg = new RandomRegressor()
      // val reg = new LinearRegressor()
      // reg.train(X_train_reg, y_train_reg)
      // val y_pred_reg: List[Double] = reg.predict(X_test_reg)
      // for (i <- 0 until Math.min(y_pred_reg.length, 10)) {
      //   println("Test instance " + i + ": " + X_test_reg(i) + " prediction %.2f  true value %.2f".format(y_pred_reg(i), y_test_reg(i)))
      // }
  }
}
