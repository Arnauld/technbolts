package org.technbolts.shibi

import org.specs.Specification
import org.slf4j.{LoggerFactory, Logger}
import org.jruby.embed.ScriptingContainer
import org.jruby.javasupport.JavaEmbedUtils.EvalUnit
import org.jruby.runtime.builtin.IRubyObject
import org.jruby.javasupport.JavaEmbedUtils
import java.io.StringWriter
import java.lang.String
import javax.script.{ScriptContext, ScriptEngine, ScriptEngineManager}

class JRubySpecs extends Specification {
  val log: Logger = LoggerFactory.getLogger(classOf[JRubySpecs])

  val NL = System.getProperty("line.separator")

  "JRuby" should {

    "be invokable within scala (jsr233)" in {
      val manager = new ScriptEngineManager
      val engine:ScriptEngine = manager.getEngineByName("jruby")
      val output = new StringWriter
      val context = engine.getContext
      context.setWriter(output)
      val result: AnyRef = engine.eval("puts \"Hello World!\"")
      output.toString must equalTo("Hello World!"+NL)
    }

    "be invokable within scala (core)" in {
      val output = new StringWriter
      val container = new ScriptingContainer
      container.setWriter(output)
      container.runScriptlet("puts \"Hello World!\"")
      output.toString must equalTo("Hello World!"+NL)
    }

    "be invokable within scala using unit and variable" in {
      val container = new ScriptingContainer
      val script = """
              def message
                valuea = $msga
                "message: #{$msg} #{valuea}"
              end
              message
              """
      container.put("msg", "What's up?")
      container.put("msga", "Doc!!")
      val unit:EvalUnit = container.parse(script)
      var ret:IRubyObject = unit.run

      val rubyA: AnyRef = JavaEmbedUtils.rubyToJava(ret)
      rubyA must equalTo("message: What's up? Doc!!")

      container.put("msg", "Fabulous!")
      ret = unit.run
      JavaEmbedUtils.rubyToJava(ret) must_== "message: Fabulous! Doc!!"

      container.put("msg", "That's the way you are.")
      ret = unit.run
      JavaEmbedUtils.rubyToJava(ret) must_== "message: That's the way you are. Doc!!"
    }

    "be invokable within scala using unit" in {
      val container = new ScriptingContainer
      val script = """
              def message
                "message: eh oh!"
              end
              message
              """
      val unit:EvalUnit = container.parse(script)
      var ret:IRubyObject = unit.run
      JavaEmbedUtils.rubyToJava(ret) must_== "message: eh oh!"
    }

  }
}