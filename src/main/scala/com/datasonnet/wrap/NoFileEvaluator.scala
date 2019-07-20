package com.datasonnet.wrap

import sjsonnet.{Evaluator, Expr, Scope}

class NoFileEvaluator(parseCache: collection.mutable.Map[String, fastparse.Parsed[Expr]], originalScope: Scope) extends Evaluator(parseCache, originalScope, Map(), os.pwd, Some(Set()), None) {


  // TODO add import support for named imports
  // Currently all imports are blocked by the empty allowedImports
  // (Though I believe the implementation can be used to discover what files are present, as it still does that lookup)
  // Note: we can't get what we need by passing an `importer` because that only does path discovery; os.read is still called

  // make this return a dummy path
  // override def resolveImport(scope: Scope, value: String, offset: Int): os.Path

  // and make this just do a path -> String lookup in a Map we'll provide from config
  // override def importString(scope: Scope, offset: Int, value: String, p: os.Path): String
}
