package com.datasonnet.wrap

import sjsonnet.{Evaluator, Expr, Path, OsPath}

class NoFileEvaluator(jsonnet: String, path: Path, parseCache: collection.mutable.Map[String, fastparse.Parsed[(Expr, Map[String, Int])]]) extends Evaluator(parseCache, Map(), path, (Path, String) => None) {




  this.loadedFileContents(path) = jsonnet
  // okay, I think part of overriding error handling (including line numbering) is
  // to override `implicit def evalScope: EvalScope = this` in Evaluator, if we can (probably?)
  // alternatively, we could just let it do it by handling the import bits

  // TODO add import support for named imports
  
}
