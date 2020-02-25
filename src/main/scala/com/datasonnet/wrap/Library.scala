package com.datasonnet.wrap

import sjsonnet.Expr.Member.Visibility
import sjsonnet._


object Library {
  def library(functions: (String, Val.Func)*): Val.Obj = new Val.Obj(
    functions
      .map{
        case (k, v) =>
          (
            k,
            Val.Obj.Member(
              false,
              Visibility.Hidden,
              (self: Val.Obj, sup: Option[Val.Obj], _, _) => v
            )
          )
      }
      .toMap,
    _ => (),
    None
  )
}