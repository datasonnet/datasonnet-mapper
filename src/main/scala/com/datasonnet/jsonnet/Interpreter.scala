package com.datasonnet.jsonnet

/*-
 * Copyright 2019-2022 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.io.{PrintWriter, StringWriter}

import fastparse.Parsed
import com.datasonnet.jsonnet.Expr.Params

/**
  * Wraps all the machinery of evaluating Jsonnet source code, from parsing to
  * evaluation to materialization, into a convenient wrapper class.
  */
class Interpreter(parseCache: collection.mutable.Map[String, fastparse.Parsed[(Expr, Map[String, Int])]],
                  extVars: Map[String, ujson.Value],
                  tlaVars: Map[String, ujson.Value],
                  wd: Path,
                  importer: (Path, String) => Option[(Path, String)],
                  preserveOrder: Boolean = false) {

  val evaluator = new Evaluator(
    parseCache,
    extVars,
    wd,
    importer,
    preserveOrder
  )

  def interpret(txt: String, path: Path): Either[String, ujson.Value] = {
    interpret0(txt, path, ujson.Value)
  }
  def interpret0[T](txt: String,
                    path: Path,
                    visitor: upickle.core.Visitor[T, T]): Either[String, T] = {
    for{
      res <- parseCache.getOrElseUpdate(txt, fastparse.parse(txt, Parser.document(_))) match{
        case f @ Parsed.Failure(l, i, e) => Left("Parse error: " + f.trace().msg)
        case Parsed.Success(r, index) => Right(r)
      }
      (parsed, nameIndices) = res
      _ = evaluator.loadedFileContents(path) = txt
      res0 <-
        try Right(
          evaluator.visitExpr(parsed)(
            Std.scope(nameIndices.size + 1),
            new FileScope(path, nameIndices)
          )
        )
        catch{case e: Throwable =>
          val s = new StringWriter()
          val p = new PrintWriter(s)
          e.printStackTrace(p)
          p.close()
          Left(s.toString.replace("\t", "    "))
        }
      res = res0 match{
        case f: Val.Func =>
          f.copy(params = Params(f.params.args.map{ case (k, default, i) =>
            (k, tlaVars.get(k) match{
              case None => default
              case Some(v) => Some(Materializer.toExpr(v))
            }, i)
          }))
        case x => x
      }
      json <-
        try Right(Materializer.apply0(res, visitor)(evaluator))
        catch{
          case Error.Delegate(msg) => Left(msg)
          case e: Throwable =>
            val s = new StringWriter()
            val p = new PrintWriter(s)
            e.printStackTrace(p)
            p.close()
            Left(s.toString.replace("\t", "    "))
        }
    } yield json
  }
}
