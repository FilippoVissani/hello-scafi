package examples

import it.unibo.scafi.incarnations.BasicAbstractIncarnation

// 1. Define (or import) an incarnation, which provides an instantiation of types and other classes to import
// Note: mix StandardLibrary into the incarnation to enable use of standard library traits
object MyIncarnation extends BasicAbstractIncarnation

// 2. Bring into scope the stuff from the chosen incarnation
import examples.MyIncarnation._

// 3. Define an "aggregate program" using the ScaFi DSL by extending AggregateProgram and specifying a "main" expression
class GradientProgram extends AggregateProgram {
  def isSource: Boolean = sense("source")
  /* hop gradient */
  override def main(): Any = rep(Double.PositiveInfinity) { d =>
    mux(isSource)(0.0)(foldhoodPlus(Double.PositiveInfinity)(Math.min)(nbr(d) + 1.0))
  }
}

// 4. In your program, implement an "execution loop" whereby your device or system executes the aggregate program
object HelloScafi extends App {
  val program = new GradientProgram()
  // Import standard sensors name define in incarnation
  val sensorsNames = new StandardSensorNames {}
  import sensorsNames._
  // Now let's build a simplified system with sequential execution just to illustrate the execution model
  // Suppose the following topology: [1] -- [2] -- [3] -- [4] -- [5]
  // And that the source of the gradient is the device no. 2
  // Then, the expected result once the gradient has stabilised is: {1 -> 1, 2 -> 0, 3 -> 1, 4 -> 2, 5 -> 3}
  case class DeviceState(
                          self: ID,
                          exports: Map[ID, EXPORT],
                          localSensors: Map[CNAME, Any],
                          nbrSensors: Map[CNAME, Map[ID, Any]]
                        )
  val devices = 1 to 5
  var state: Map[ID, DeviceState] = (for {
    d <- devices
    nbrs = Seq(d - 1, d, d + 1).filter(n => n > 0 && n < 6)
    localSensor = Map[CNAME, Any]("source" -> false)
    neighboursSensors = Map[CNAME, Map[ID, Any]](
      NBR_RANGE -> (nbrs.toSet[ID].map(nbr => nbr -> Math.abs(d - nbr).toDouble)).toMap
    )
  } yield d -> DeviceState(d, Map.empty[ID, EXPORT], localSensor, neighboursSensors)).toMap
  val sourceId = 2
  state = state + (sourceId -> state(sourceId).copy(localSensors = state(sourceId).localSensors + ("source" -> true)))
  // The following cycle performs the scheduling of rounds and simulates communication by writing on `state`
  val scheduling = devices ++ devices ++ devices ++ devices ++ devices // run 5 rounds each, in a round-robin fashion
  for (d <- scheduling) {
    val ctx = factory.context(
      selfId = d,
      exports = state(d).exports,
      lsens = state(d).localSensors,
      nbsens = state(d).nbrSensors
    )
    println(s"RUN: DEVICE $d")
    println(s"\tCONTEX:")
    println(s"\t\tEXPORTS:")
    state(d).exports.foreach(e => {
      println(s"\t\t\tID ${e._1}:")
      e._2.paths.foreach(p => println(s"\t\t\t\t$p"))
    })
    println(s"\t\tNBR SENSORS: ${state(d).nbrSensors}")
    println(s"\t\tLOCAL SENSORS: ${state(d).localSensors}")
    val export = program.round(ctx)
    state += d -> state(d).copy(exports = state(d).exports + (d -> export)) // update d's state
    // Simulate sending of messages to neighbours
    state(d)
      .nbrSensors(NBR_RANGE)
      .keySet
      .foreach(nbr => state += nbr -> state(nbr).copy(exports = state(nbr).exports + (d -> export)))
    println(s"\tEXPORT:")
    export.paths.foreach(p => println(s"\t\t$p"))
    println(s"\tOUTPUT: ${export.root()}")
    println("---------------------")
  }
}
