package com.datasonnet.wrap

import sjsonnet.{Evaluator, Expr, Path, OsPath}

class NoFileEvaluator(jsonnet: String,
                      path: Path,
                      parseCache: collection.mutable.Map[String, fastparse.Parsed[(Expr, Map[String, Int])]],
                      importer: (Path, String) => Option[(Path, String)],
                      preserveOrder: Boolean = true) extends Evaluator(parseCache, Map(), path, importer, preserveOrder) {
  this.loadedFileContents(path) = jsonnet
}
