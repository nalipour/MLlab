import org.scalatest._

import datastructures._


class DataStructuresSuite extends FunSuite {

  test("polylist") {
    val twoTwo = List(List(2, 0), List(1, 1), List(0, 2))
    assert (DataTrafo.polyList(2, 2, Nil).toSet  === twoTwo.toSet)
    assert (DataTrafo.polyList(2, 2, Nil).length === twoTwo.length)
    val threeTwo = List(List(3, 0), List(2, 1), List(1, 2), List(0, 3), List(2, 0), List(1, 1), List(0, 2))
    assert (DataTrafo.polyList(3, 2, Nil).toSet  === threeTwo.toSet)
    assert (DataTrafo.polyList(3, 2, Nil).length === threeTwo.length)
    val threeThree = List(
      List(3, 0, 0), List(0, 3, 0), List(0, 0, 3),
      List(2, 1, 0), List(2, 0, 1), List(1, 2, 0), List(0, 2, 1), List(1, 0, 2), List(0, 1, 2),
      List(2, 0, 0), List(0, 2, 0), List(0, 0, 2),
      List(0, 1, 1), List(1, 0, 1), List(1, 1, 0),
      List(1, 1, 1)
    )
    assert (DataTrafo.polyList(3, 3, Nil).toSet  === threeThree.toSet)
    assert (DataTrafo.polyList(3, 3, Nil).length === threeThree.length)
  }

  test ("decisiontree nodes") {
    assert (new DecisionTree(1).nNodes === 1)
    assert (new DecisionTree(2).nNodes === 3)
    assert (new DecisionTree(10).nNodes === 1023)
  }

  test ("decisiontree entries") {
    assert (new DecisionTree(1).tree === List(new DecisionNode(0)))
    assert (new DecisionTree(5).tree(4) === new DecisionNode(4))
  }

}