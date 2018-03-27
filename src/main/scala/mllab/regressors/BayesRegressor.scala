package regressors

import breeze.linalg._

import datastructures._
import plotting._
import utils._


/** Bayes regressor
 *
 * following https://stats.stackexchange.com/questions/252577/bayes-regression-how-is-it-done-in-comparison-to-standard-regression
 *
 * The priors can be set using the priorPars parameter. If left empty, random values will be used.
 * e.g. List(List(2), List(0, 1), List(1, 3)) for a posterior width 2 and Gaussian priors for the intercept (mean 0, sigma 1) and
 * the other weights (mean 1, sigma 3)
 *
 * @param degree Order of polynomial features to add to the instances (1 for no addition)
 * @param model The shape of the prior function
 * @param priorPars Parameters of the prior probability density functions
 * @param randInit If set to true, initialize the prior pdf parameters randomly
 */
class BayesRegressor(degree: Int=1, model: String= "gaussian", priorPars: List[List[Double]]= Nil, randInit: Boolean= false) extends Regressor {

  val name: String = "BayesRegressor"

  /** The weight vector to be determined by the training */
  var weight: List[Double] = Nil
  /** The posterior width to be determined by the training */
  var width: Double = 0

  /** The probability distribution for the weight priors */
  def priorFunc(model: String, x: Double, params: List[Double]): Double =
    if (model == "gaussian") Maths.normal(x, params.head, params.last)
    else if (model == "rectangular") Maths.rectangular(x, params.head - params.last, params.head + params.last)
    else throw new NotImplementedError("model " + model + " is not implemented")

  /** Takes the log with a lower limit */
  def finiteLog(x: Double): Double =
    if (x == 0) -10000 else Math.log(x)

  /** Converts a list of logarithmic likelihoods to probabilities */
  def logToProb(logs: List[Double]): List[Double] = {
    val maxLog = logs.max
    val subtractedLogs = logs.map(_ - maxLog)
    val precision = 1e-16
    val threshold = Math.log(precision) - Math.log(logs.length)
    val exponents = subtractedLogs.map(l => if (l < threshold) 0 else Math.exp(l))
    val sumExponents = exponents.sum
    val binScaleFactor = 1.0
    val probabs = exponents.map(_ * binScaleFactor / sumExponents)
    probabs
  }

  /** Determines the function maximum by random walks */
  def optimize(func: (Double, List[Double]) => Double, startParams: List[Double], startRanges: List[List[Double]]): Tuple2[Double, List[Double]] = {

    def maximize(count: Int, maximum: Double, params: List[Double], ranges: List[List[Double]]): List[Double] = {
      val nSteps = 1000
      val numberDimensions: Int = ranges.length
      if (count == nSteps) {
        println(s"- final step $count: optimum %.3f, params ".format(maximum) +
        "(" + params.map(p => Maths.round(p, 3)).mkString(", ") + ")"
        )
        params
      }
      else {
        if (count % 100 == 0 || (count < 50 && count % 10 == 0) || (count < 5))
          println(s"- optimization step $count: optimum %.3e, params ".format(maximum) +
          "(" + params.map(p => Maths.round(p, 3)).mkString(", ") + ")"
        )
        val dimension: Int = scala.util.Random.nextInt(numberDimensions)
        val sign: Int = scala.util.Random.nextInt(2) * 2 - 1
        val step: Double = 1.0 * sign * (ranges(dimension)(1) - ranges(dimension).head) / 100
        // println(s"Step $count: step %.3f in dimension $dimension".format(step))
        val newParams: List[Double] = params.zipWithIndex.map{case (p, i) => if (i == dimension) p + step else p}
        val newMaximum: Double = func(newParams.head, newParams.tail)
        // if (newMaximum > maximum) println("New maximum " + maximum + " at " + params)
        if (newMaximum > maximum) maximize(count+1, newMaximum, newParams, ranges)
        else maximize(count+1, maximum, params, ranges)
      }
    }

    val params: List[Double] = maximize(0, Double.MinValue, startParams, startRanges)
    (params.head, params.tail)
  }

  def _train(X: List[List[Double]], y: List[Double]): Unit = {
    require(X.length == y.length, "both arguments must have the same length")
    val nFeatures = X.head.length

    /** Posterior width prior */
    val widthPrior: Double =
      if (priorPars.isEmpty)
        if (randInit) 1.0 * scala.util.Random.nextInt(3) + 1
        else 1.0
      else priorPars.head.head

    /** Parameter weight mean prior */
    val weightPrior: List[Double] =
      if (priorPars.isEmpty)
        if (randInit) (for (i <- 0 to nFeatures) yield 1.0 * scala.util.Random.nextInt(7) - 3).toList
        else (for (i <- 0 to nFeatures) yield Math.ceil(1.0 * i / 2) * ((i % 2) * 2 - 1)).toList
      else priorPars.tail.map(_.head)

    /** Parameter weight width prior */
    val weightPriorWidth: List[Double] =
      if (priorPars.isEmpty)
        if (randInit) (for (i <- 0 to nFeatures) yield 1.0 * scala.util.Random.nextInt(3) + 1).toList
        else (for (i <- 0 to nFeatures) yield 1.0 * (i % 4 + 1)).toList
      else priorPars.tail.map(_.last)

    /** Evaluates the weight prior for the given weight: p(W) */
    val evalWeightPrior = (b: List[Double]) => {(b zip (weightPrior zip weightPriorWidth)).map{case (bi, ms) => priorFunc(model, bi, List(ms._1, ms._2))}}

    /** Evaluates the width prior for the given width: p(s) */
    val evalWidthPrior = (s: Double) => {Maths.rectangular(s, 0, widthPrior)}

    /** Evaluates the likelihood given the data: p(X, y | W, s) */
    val likelihood = (s: Double, W: List[Double]) => {
      (X zip y).map{case (xi, yi) => finiteLog(Maths.normal(yi, Maths.dot(W, 1 :: xi), s))}.sum
    }
    /** Evaluates the posterior given the data: p(W, s | X, y) */
    val posterior = (s: Double, W: List[Double]) => {
      likelihood(s, W) + evalWeightPrior(W).map(wp => finiteLog(wp)).sum + finiteLog(evalWidthPrior(s))
    }
    // determine maximum likelihood parameters
    val intervals: Double = 3.0
    val rangeWeight: List[List[Double]] = (weightPrior zip weightPriorWidth).map{case (m, s) => List(m - intervals * s, m + intervals * s)}
    val rangeWidth: List[List[Double]] = List(List(0, widthPrior))
    val ranges: List[List[Double]] = rangeWidth ::: rangeWeight
    val startParams: List[Double] = ranges.map(r => Maths.mean(r))
    val (optimalWidth: Double, optimalWeight: List[Double]) = optimize(posterior, startParams, ranges)
    weight = optimalWeight
    width = optimalWidth

    println("Final estimated parameter means for Y <- N(B.head + B.tail * X, S):")
    println("B = " + weight.map(Maths.round(_, 3)))
    println("S = %.3f".format(width))

    // create x-axis for plotting
    val equiVec: DenseVector[Double] = linspace(ranges.flatten.min, ranges.flatten.max, 200)
    val xAxis: List[Double] = (for (i <- 0 until equiVec.size) yield equiVec(i)).toList

    // plot priors
    val xAxisPerWeight = List.fill(nFeatures+1)(xAxis)
    val valsWeight = xAxisPerWeight.transpose.map(evalWeightPrior(_)).transpose.map(xAxis zip _)
    val valsWidth = xAxis zip (xAxis.map(evalWidthPrior(_)))
    val vals = valsWidth :: valsWeight
    val names = "S" :: (for (i <- 0 to nFeatures) yield "W" + i).toList
    Plotting.plotCurves(vals, names, xlabel= "Parameter value", ylabel= "Prior probability", name= "plots/reg_Bayes_priors.pdf")

    // create x-axis for plotting final likelihoods
    val results: List[Double] = width :: weight
    val space = (results.max - results.min) * 0.1
    val equiVecPost: DenseVector[Double] = linspace(results.min - space, results.max + space, 1000)
    val xAxisPost: List[Double] = (for (i <- 0 until equiVecPost.size) yield equiVecPost(i)).toList

    // plot likelihoods
    val likelihoodWeightPlots =
      (for (i <- 0 to nFeatures) yield xAxisPost zip logToProb(xAxisPost.map(eq => {
        val allButOne: List[Double] = (for (j <- 0 to nFeatures) yield if (i == j) eq else weight(j)).toList
        likelihood(width, allButOne)
      }
    ))).toList
    val likelihoodWidthPlot = xAxisPost zip logToProb(xAxisPost.map(eq => likelihood(eq, weight)))
    val likelihoodPlots = likelihoodWidthPlot :: likelihoodWeightPlots
    Plotting.plotCurves(likelihoodPlots, names, xlabel= "Parameter value", ylabel= "Likelihood probability", name= "plots/reg_Bayes_likelihoods.pdf")

    // plot posteriors
    val posteriorWeightPlots =
      (for (i <- 0 to nFeatures) yield xAxisPost zip logToProb(xAxisPost.map(eq => {
          val allButOne: List[Double] = (for (j <- 0 to nFeatures) yield if (i == j) eq else weight(j)).toList
          posterior(width, allButOne)
        }
      ))).toList
    val posteriorWidthPlot = xAxisPost zip logToProb(xAxisPost.map(eq => posterior(eq, weight)))
    val posteriorPlots = posteriorWidthPlot :: posteriorWeightPlots
    Plotting.plotCurves(posteriorPlots, names, xlabel= "Parameter value", ylabel= "Posterior probability", name= "plots/reg_Bayes_posteriors.pdf")
  }

  def _predict(X: List[List[Double]]): List[Double] =
    for (instance <- X) yield Maths.dot(weight, 1 :: instance)

  def predict(X: List[List[Double]]): List[Double] =
    _predict(DataTrafo.addPolyFeatures(X, degree))

  def train(X: List[List[Double]], y: List[Double]): Unit =
    _train(DataTrafo.addPolyFeatures(X, degree), y)

}
