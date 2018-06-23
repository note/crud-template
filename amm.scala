object amm extends App {
  import ammonite.ops.Path

  println("pwd: " + ammonite.ops.pwd)

  val predefPath = Path("amm_predef.scala", ammonite.ops.pwd)

  ammonite.Main(predefFile = Some(predefPath)).run()
}