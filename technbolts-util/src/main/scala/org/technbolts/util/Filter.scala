package org.technbolts.util

import java.io._
import io.Source

object Filter {
  private def replace(text: String, prop: String, value: String): String =
    text.replaceAll("\\$\\{" + prop + "\\}", value)

  def filter(props: Map[String, String], text: String): String =
    props.foldLeft(text) {
      case (t, (from ,to)) => replace(t, from, to)
    }

  def filter(props: Map[String, String])(in: InputStream, os: OutputStream, encoding:String):Unit = {
    val out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(os, encoding)))

    Source.fromInputStream(in, encoding).getLines.foreach {
      line => out.print(filter(props, line))
    }

    out.flush
    out.close
  }
}