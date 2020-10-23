package engine.core.control.promise


import scala.reflect.api.Trees
import scala.reflect.macros.whitebox


object SyntaxChecker {

  def handlerImpl(c: whitebox.Context)(eventHandler: c.Tree): c.Tree = {
    import c.universe._
//    eventHandler match {
//      case q"{ case ..$cases }" =>
//        cases.foreach {
//          syntaxCheck(c)(_)
//        }
//      case _ =>
//        c.abort(c.macroApplication.pos, "The syntax for event handler is not correct, please check.")
//    }
    q""" promiseHandler = $eventHandler orElse promiseHandler """
  }

  def syntaxCheck(c: whitebox.Context)(oneCase: Trees#Tree):Unit = {
    import c.universe._
    oneCase match{
      case CaseDef(part,guard,body) =>
        if(!guard.isEmpty)c.abort(guard.pos," No if-guard is allowed when writing the handler")
        part match{
          case Apply(x,y) =>
            if(x.tpe.finalResultType.baseType(typeOf[InternalPromise[_]].typeSymbol).typeArgs.head == typeOf[Nothing]){
              assertNoReturn(c)(body, x.tpe.finalResultType.typeSymbol.name.toString)
            }else{
              assertUniqueReturnInEveryBranch(c)(body, x.tpe.finalResultType.typeSymbol.name.toString)
            }
        }
      case _ =>
    }
  }


  def assertNoReturn(c: whitebox.Context)(body: c.Tree, caseName:String):Unit = {
    import c.universe._
    body.foreach{
      case Apply(Select(t, TermName(methodName)), _) if methodName == "returning" =>
        c.abort(t.pos, s"returning function call cannot be used inside the handler of $caseName")
      case _ =>
        //skip
    }
  }

  def assertUniqueReturnInEveryBranch(c: whitebox.Context)(body: c.Tree, caseName:String):Unit = {
    import c.universe._

  }


}
