package scalit.tangle
import scalit.markup._

case class CodeChunk(blocknumber: Int, linenumber: Int,
           content: Stream[Line],blockname: String,
           next: Option[CodeChunk]) extends scalit.markup.StringRefs.StringRef {
  
  import StringRefs._

  def append(that: CodeBlock): CodeChunk = next match {
    case None =>
      CodeChunk(this.blocknumber,
                this.linenumber,
                this.content,
                this.blockname,
                Some(CodeChunk(that.blocknumber,
                    that.linenumber,
                    that.content,
                    that.blockname,None)))
    case Some(next) =>
      CodeChunk(this.blocknumber,
                this.linenumber,
                this.content,
                this.blockname,
                Some(next append that))
  }

  def superStringRefForm(codeBlocks: scala.collection.immutable.Map[String,CodeBlock]) = {
  def cbAcc(ls: Stream[Line], acc: String,
        begin: Int, off: Int): Stream[StringRef] =
ls match {
  case Stream.cons(first,rest) => first match {
    case NewLine => cbAcc(rest, acc + "\n", begin, off + 1)
    case TextLine(content) =>
      cbAcc(rest, acc + content, begin, off)
    case Use(usename) => {
      val cb = codeBlocks get usename match {
        case Some(codeBlock) => codeBlock
        case None =>
          System.err.println("Did not find block " +
                         usename)
          sys.exit(1)
      }
      Stream.cons(RealString(acc,begin,off),
      Stream.cons(BlockRef(cb),cbAcc(rest,"",off,off)))
    }
    case other => sys.error("Unexpected line: " + other)
}
  case Stream.Empty => acc match {
    case "" => Stream.Empty
    case s  => Stream.cons(RealString(s,begin,off),Stream.Empty)
}
}

cbAcc(content,"",linenumber,linenumber)
}

  def stringRefForm(codeChunks: scala.collection.immutable.Map[String,CodeBlock]):
    Stream[StringRef] = next match {
      case None => superStringRefForm(codeChunks)
      case Some(el) => Stream.concat(
        superStringRefForm(codeChunks),
        el.stringRefForm(codeChunks))
    }
}


import scala.collection.immutable.{Map,HashMap}
case class ChunkCollection(cm: Map[String,CodeChunk],
                         filename: String) {

  import StringRefs._

  def stringCodeChunkToStringCodeBlock(kv : (String,CodeChunk)) = {
    kv match {
      case (key,CodeChunk(bn,ln,cont,bname,_)) => (key,CodeBlock(bn,ln,cont,bname))
    }
  }

  def serialize(chunkname: String): String =
    cm get chunkname match {
      case None => sys.error("Did not find chunk " + chunkname)
      case Some(el) => flatten(el.stringRefForm(cm map stringCodeChunkToStringCodeBlock))
    }

  def addBlock(that: CodeBlock): ChunkCollection =
    cm get that.blockname match {
      case None => ChunkCollection(cm +
        (that.blockname ->
         CodeChunk(that.blocknumber,
                  that.linenumber,
                  that.content,
                  that.blockname,
                  None)),filename)
      case Some(el) => ChunkCollection(cm +
        (that.blockname ->
         el.append(that)),filename)
    }

  def addBlocks(those: Stream[CodeBlock]): ChunkCollection =
    (those foldLeft this) {
      (acc: ChunkCollection, n: CodeBlock) =>
        acc.addBlock(n)
    }

  def expandRefs(str: Stream[StringRef]): Stream[RealString] =
    str match {
      case Stream.Empty => Stream.Empty
      case Stream.cons(first,rest) =>
        first match {
          case r @ RealString(_,_,_) =>
            Stream.cons(r,expandRefs(rest))
          case BlockRef(sref) =>
            Stream.concat(
              expandRefs(cm(sref.blockname).stringRefForm(cm map stringCodeChunkToStringCodeBlock)),
              expandRefs(rest))
          case other => sys.error("Unexpected string ref: " + other)
        }
    }

  def expandedStream(chunkname: String): Stream[RealString] =
    cm get chunkname match {
      case None => sys.error("Did not find chunk " + chunkname)
      case Some(el) => expandRefs(el.stringRefForm(cm map stringCodeChunkToStringCodeBlock))
    }


  private def flatten(str: Stream[StringRef]): String = {
    val sb = new StringBuffer
    expandRefs(str) foreach {
      case RealString(content,_,_) => sb append content
    }
    sb.toString
  }
}

//case class emptyChunkCollection(fn: String)
//     extends ChunkCollection(Map(),fn)


object Tangle {
  def main(args: Array[String]) = {
    import scalit.util.LiterateSettings

    val ls = new LiterateSettings(args)

    val chunksToTake = ls.settings get "-r" match {
      case None => Nil
      case Some(cs) => cs.reverse
    }

    val out = ls.output

    chunksToTake match {
      case Nil =>
        ls.chunkCollections foreach {
          cc => out.println(cc.serialize("*"))
        }

      case cs =>
        cs foreach {
          chunk =>
            ls.chunkCollections foreach {
              cc =>
                try {
                  out.println(cc.serialize(chunk))
                } catch {
                  case e : Throwable => ()
                }
            }
        }
    }
  }
}

