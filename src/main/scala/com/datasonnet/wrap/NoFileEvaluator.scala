package com.datasonnet.wrap

/*-
 * Copyright 2019-2023 the original author or authors.
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
import com.datasonnet.jsonnet.{Evaluator, Expr, Path}
import ujson.Value

class NoFileEvaluator(jsonnet: String,
                      path: Path,
                      parseCache: collection.mutable.Map[String, fastparse.Parsed[(Expr, Map[String, Int])]],
                      importer: (Path, String) => Option[(Path, String)],
                      preserveOrder: Boolean = true,
                      defaultValue: Value) extends Evaluator(parseCache, Map(), path, importer, preserveOrder, defaultValue) {
  this.loadedFileContents(path) = jsonnet
}
