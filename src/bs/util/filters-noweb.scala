package scalit.util

import scalit.markup.Line
abstract class MarkupFilter extends
  (Stream[Line] => Stream[Line]) {
    def externalFilter(command: String,
                       lines: Stream[Line]): Stream[Line] = {
      val p = Runtime.getRuntime().exec(command)

      val procWriter = new java.io.PrintWriter(p.getOutputStream())

      lines foreach procWriter.println

      procWriter.close()
      p.waitFor()
      scalit.util.conversions.linesFromMarkupInput(p.getInputStream())
    }

}


class tee extends MarkupFilter {
  def apply(lines: Stream[Line]): Stream[Line] = {
    externalFilter("tee out",lines)
  }
}

class simplesubst extends MarkupFilter {
  def apply(lines: Stream[Line]): Stream[Line] = {
    import scalit.markup.TextLine
    lines map {
      case TextLine(cont) =>
        TextLine(cont.replace(" " + "LaTeX "," \\LaTeX "))
      case unchanged => unchanged
    }
  }
}


import scalit.markup.Block
abstract class BlockFilter extends
  (Stream[Block] => Stream[Block]) {
}


class stats extends BlockFilter {
  def apply(blocks: Stream[Block]): Stream[Block] = {
    val (doclines,codelines) = collectStats(blocks)

    import scalit.markup.{TextLine,NewLine}
    val content =
      Stream.cons(NewLine,
      Stream.cons(TextLine("Documentation lines: " +
                  doclines +
                  ", Code lines: " +
                  codelines),
      Stream.cons(NewLine,Stream.Empty)))
    Stream.concat(blocks,Stream.cons(
      scalit.markup.DocuBlock(-1,-1,content),Stream.Empty))
  }


  def collectStats(bs: Stream[Block]): (Int,Int) = {
    def collectStats0(str: Stream[Block],
                doclines: Int,
                codelines: Int): (Int,Int) = str match {
       case Stream.Empty => (doclines,codelines)
       case Stream.cons(first,rest) => first match {

         case scalit.markup.CodeBlock(_,_,lines,_) =>
           val codels = (lines foldLeft 0) {
             (acc: Int, l: scalit.markup.Line) => l match {
               case scalit.markup.NewLine => acc + 1
               case _ => acc
             }
           }
           collectStats0(rest,doclines,codelines + codels)

         case scalit.markup.DocuBlock(_,_,lines) =>
           val doculs = (lines foldLeft 0) {
             (acc: Int, l: scalit.markup.Line) => l match {
               case scalit.markup.NewLine => acc + 1
               case _ => acc
             }
           }
           collectStats0(rest,doclines + doculs, codelines)
       }
    }
    collectStats0(bs,0,0)
  }
}


