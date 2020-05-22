package com.datasonnet


import java.net.URL
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.{Date, Scanner}

import com.datasonnet
import com.datasonnet.spi.UjsonUtil
import com.datasonnet.wrap.Library.library
import sjsonnet.Expr.Member.Visibility
import sjsonnet.ReadWriter.StringRead
import sjsonnet.Std._
import sjsonnet.{Applyer, EvalScope, FileScope, Materializer, Val}

import scala.util.Random

/*
 * considerations:
 *    Math: which one is faster Java.lang.Math, or scala.math ?
 */


object DW {

  def distinctBy(array: Seq[Val.Lazy], funct: Applyer): Val ={
    val args=funct.f.params.argIndices.size>1
    val out = collection.mutable.Buffer.empty[Val.Lazy]
    if(args) { // 2 args
      for ((item, index) <- array.zipWithIndex) {

        if (!out.zipWithIndex.map { // out array does not contain item
          case (outItem, outIndex) => funct.apply(outItem, Val.Lazy(Val.Num(outIndex)))
        }.contains(funct.apply(item, Val.Lazy(Val.Num(index))))) {
          out.append(item)
        }
      }
    }
    else{ // 1 arg
      for (item <- array) {

        if (!out.map { // out array does not contain item
            case (outItem) => funct.apply(outItem)
          }.contains(funct.apply(item))) {
          out.append(item)
        }
      }
    }
    Val.Arr(out.toSeq)
  }

  //Needs to be cleaned up
  def distinctBy(obj: Val.Obj, funct: Applyer, ev: EvalScope, fs: FileScope): Val ={
    val args=funct.f.params.argIndices.size>1
    val out =scala.collection.mutable.Map[String, Val.Obj.Member]()

    if(args) { // 2 args
      for ((key, hidden) <- obj.getVisibleKeys()) {
        val outObj = new Val.Obj(out, _ => (), None)

        if(! outObj.getVisibleKeys().toSeq.map{
          case (outKey, outH) =>
            funct.apply(Val.Lazy(outObj.value(outKey, -1)(fs, ev)), Val.Lazy(Val.Str(outKey)))
        }.contains(funct.apply(Val.Lazy(obj.value(key, -1)(fs, ev)), Val.Lazy(Val.Str(key))))){
          out.+=(key -> Val.Obj.Member(add = false, Visibility.Normal, (_, _, _, _) => obj.value(key, -1)(fs, ev)))
        }
      }
    }
    else{ //1 arg
      for ((key, hidden) <- obj.getVisibleKeys()) {
        val outObj = new Val.Obj(out, _ => (), None)

        if(! outObj.getVisibleKeys().toSeq.map{
          case (outKey, outH) =>
            funct.apply(Val.Lazy(outObj.value(outKey, -1)(fs, ev)))
        }.contains(funct.apply(Val.Lazy(obj.value(key, -1)(fs, ev))))){
          out.+=(key -> Val.Obj.Member(add = false, Visibility.Normal, (_, _, _, _) => obj.value(key, -1)(fs, ev)))
        }
      }
    }
    new Val.Obj(out, _ => (), None)
  }

  def filter(array: Seq[Val.Lazy], funct: Applyer): Val ={
    val args = funct.f.params.allIndices.size>1
    Val.Arr(
      if(args)
        array.zipWithIndex.filter({
          case (lazyItem, index) => (funct.apply(lazyItem, Val.Lazy(Val.Num(index))) == Val.True)
        }).map(_._1)
    else
      array.filter({
        case (lazyItem) => (funct.apply(lazyItem) == Val.True)
      })
    )
  }

  def filterObject(obj: Val.Obj, func: Applyer, ev: EvalScope, fs: FileScope): Val ={
    val args=func.f.params.allIndices.size
    val out = scala.collection.mutable.Map[String, Val.Obj.Member]()
    args match {
      case 3 =>

        new Val.Obj(
          scala.collection.mutable.Map(
            obj.getVisibleKeys().zipWithIndex.toSeq.collect({
              case ((key,h),index)
                if (func.apply(Val.Lazy(obj.value(key, -1)(fs, ev)), Val.Lazy(Val.Str(key)), Val.Lazy(Val.Num(index))) == Val.True) =>
                  (key -> Val.Obj.Member(add = false, Visibility.Normal, (_, _, _, _) => obj.value(key, -1)(fs, ev)))
            }): _*),
          _ => (), None)
      case 2 =>
        new Val.Obj(
          scala.collection.mutable.Map(
            obj.getVisibleKeys().zipWithIndex.toSeq.collect({
              case ((key,h),index)
                if (func.apply(Val.Lazy(obj.value(key, -1)(fs, ev)), Val.Lazy(Val.Str(key))) == Val.True) =>
                (key -> Val.Obj.Member(add = false, Visibility.Normal, (_, _, _, _) => obj.value(key, -1)(fs, ev)))
            }): _*),
          _ => (), None)
      case 1 =>
        new Val.Obj(
          scala.collection.mutable.Map(
            obj.getVisibleKeys().zipWithIndex.toSeq.collect({
              case ((key,h),index)
                if (func.apply(Val.Lazy(obj.value(key, -1)(fs, ev))) == Val.True) =>
                (key -> Val.Obj.Member(add = false, Visibility.Normal, (_, _, _, _) => obj.value(key, -1)(fs, ev)))
            }): _*),
          _ => (), None)
    }
  }

  def flatMap(array: Seq[Val.Lazy], funct: Applyer): Val ={
    val args = funct.f.params.allIndices.size>1
    val out = collection.mutable.Buffer.empty[Val.Lazy]
    if(args) { // 2 args
      for (v <- array) {
        v.force match {
          case Val.Arr(inner) =>
            out.appendAll(inner.zipWithIndex.map({
              case (it, ind) => Val.Lazy(funct.apply(it, Val.Lazy(Val.Num(ind))))
            }))
          case _ => throw new IllegalArgumentException(
            "Expected Array of Arrays, got: Array of " + v.force.prettyName);
        }
      }
    }
    else{ //  1 arg
      for(v <- array){
        v.force match {
          case Val.Arr(inner) =>
            out.appendAll(inner.map({
              case (it) => Val.Lazy(funct.apply(it))
            }))
          case _ =>  throw new IllegalArgumentException(
            "Expected Array of Arrays, got: Array of " + v.force.prettyName);
        }
      }
    }
    Val.Arr(out.toSeq);
  }

  def groupBy(s: Seq[Val.Lazy], funct: Applyer): Val ={
    val args=funct.f.params.allIndices.size>1
    val out = scala.collection.mutable.Map[String, Val.Obj.Member]()
    if(args) {
      for ((item, index) <- s.zipWithIndex) {

        val key = funct.apply(item, Val.Lazy(Val.Num(index)))
        if (!new Val.Obj(out, _ => (), None)
          .getVisibleKeys()
          .contains(key.cast[Val.Str].value)) {

          val array = collection.mutable.Buffer.empty[Val.Lazy]
          array.appendAll(s.zipWithIndex.collect({
            case (item2,index2) if key == funct.apply(item2, Val.Lazy(Val.Num(index2))) =>
              item2
          }))
          out += (key.cast[Val.Str].value -> Val.Obj.Member(add = false, Visibility.Normal, (_, _, _, _) => Val.Arr(array.toSeq)))
        }
      }
    }else{
      for((item) <- s){

        val key =funct.apply(item)
        if(!new Val.Obj(out, _ => (), None)
          .getVisibleKeys()
          .contains(key.cast[Val.Str].value)){

          val array = collection.mutable.Buffer.empty[Val.Lazy]
          array.appendAll(s.collect({
            case (item2) if key == funct.apply(item2) =>
              item2
          }))
          out += (key.cast[Val.Str].value -> Val.Obj.Member(add = false, Visibility.Normal, (_, _, _, _) => Val.Arr(array.toSeq)))
        }
      }
    }
    new Val.Obj(out, _ => (), None)
  }

  def groupBy(obj: Val.Obj, funct: Applyer, ev: EvalScope, fs: FileScope): Val ={
    val out = scala.collection.mutable.Map[String, Val.Obj.Member]()
    val args = funct.f.params.allIndices.size>1
    if(args) {
      for ((key, hidden) <- obj.getVisibleKeys()) {
        val functKey =funct.apply(Val.Lazy(obj.value(key, -1)(fs, ev)), Val.Lazy(Val.Str(key)))

        if (!new Val.Obj(out, _ => (), None)
          .getVisibleKeys()
          .contains(functKey.cast[Val.Str].value)) {

          val currentObj = scala.collection.mutable.Map[String, Val.Obj.Member]()
          currentObj.addAll(obj.getVisibleKeys().collect({
            case (key2,hidden2) if functKey == funct.apply(Val.Lazy(obj.value(key2, -1)(fs, ev)), Val.Lazy(Val.Str(key2))) =>
              (key2 -> Val.Obj.Member(add = false, Visibility.Normal, (_, _, _, _) => obj.value(key2, -1)(fs, ev)))
          }))
          out += (functKey.cast[Val.Str].value -> Val.Obj.Member(add = false, Visibility.Normal, (_, _, _, _) => new Val.Obj(currentObj, _ => (), None)))
        }
      }
    }
    else{
      for((key,hidden) <- obj.getVisibleKeys()){
        val functKey =funct.apply(Val.Lazy(obj.value(key,-1)(fs, ev)))

        if(!new Val.Obj(out, _ => (), None)
          .getVisibleKeys()
          .contains(functKey.cast[Val.Str].value)){

          val currentObj = scala.collection.mutable.Map[String, Val.Obj.Member]()
          currentObj.addAll(obj.getVisibleKeys().collect({
            case (key2,hidden2) if functKey == funct.apply(Val.Lazy(obj.value(key2, -1)(fs, ev))) =>
              (key2 -> Val.Obj.Member(add = false, Visibility.Normal, (_, _, _, _) => obj.value(key2, -1)(fs, ev)))
          }))
          out += (functKey.cast[Val.Str].value -> Val.Obj.Member(add = false, Visibility.Normal, (_, _, _, _) => new Val.Obj(currentObj, _ => (), None)))
        }
      }
    }
    new Val.Obj(out, _ => (), None)
  }

  def map(array: Seq[Val.Lazy], funct: Applyer): Val ={
    val args = funct.f.params.allIndices.size>1
    Val.Arr(
      if(args) { //2 args
        array.zipWithIndex.map{
          case(item, index) => Val.Lazy(funct.apply(item, Val.Lazy(Val.Num(index))))
        }
      } else { // 1 arg
        array.map{
          case(item) =>Val.Lazy(funct.apply(item))
        }
      }
    )
  }

  def mapObject(obj: Val.Obj, funct: Applyer, ev: EvalScope, fs: FileScope): Val ={
    val args=funct.f.params.allIndices.size
    val out = scala.collection.mutable.Map[String, Val.Obj.Member]()
    args match {
      case 3 =>
        for (((key, hidden), index) <- obj.getVisibleKeys().zipWithIndex) {
          funct.apply(Val.Lazy(obj.value(key, -1)(fs, ev)), Val.Lazy(Val.Str(key)), Val.Lazy(Val.Num(index))) match {
            case s: Val.Obj =>
                out.addAll(s.getVisibleKeys().map{
                  case (sKey, sHidden) =>  (sKey -> Val.Obj.Member(add = false, Visibility.Normal, (_, _, _, _) => s.value(sKey, -1)(fs, ev)))
                })
            case i => throw new IllegalArgumentException(
              "Function must return an object, got: " + i.prettyName);
          }
        }
        new Val.Obj(out, _ => (), None)
      case 2 =>
        for ((key, hidden) <- obj.getVisibleKeys()) {
          funct.apply(Val.Lazy(obj.value(key, -1)(fs, ev)), Val.Lazy(Val.Str(key))) match {
            case s: Val.Obj =>
              out.addAll(s.getVisibleKeys().map{
                case (sKey, sHidden) =>  (sKey -> Val.Obj.Member(add = false, Visibility.Normal, (_, _, _, _) => s.value(sKey, -1)(fs, ev)))
              })
            case i => throw new IllegalArgumentException(
              "Function must return an object, got: " + i.prettyName);
          }
        }
        new Val.Obj(out, _ => (), None)
      case 1 =>
        for ((key, hidden) <- obj.getVisibleKeys()) {
          funct.apply(Val.Lazy(obj.value(key, -1)(fs, ev))) match {
            case s: Val.Obj =>
              out.addAll(s.getVisibleKeys().map{
                case (sKey, sHidden) =>  (sKey -> Val.Obj.Member(add = false, Visibility.Normal, (_, _, _, _) => s.value(sKey, -1)(fs, ev)))
              })
            case i => throw new IllegalArgumentException(
              "Function must return an object, got: " + i.prettyName);
          }
        }
        new Val.Obj(out, _ => (), None)
    }
  }

  def orderBy(array: Seq[Val.Lazy], funct: Applyer): Val ={
    val args = funct.f.params.allIndices.size>1
    if(args) {
      Val.Arr(
        array.zipWithIndex.sortBy(
            (it) => funct.apply(it._1, Val.Lazy(Val.Num(it._2))).toString
        ).map(_._1))
    }
    else{
      Val.Arr(array.sortBy((it) => funct.apply(it).toString))
    }
  }

  def orderBy(obj: Val.Obj, funct: Applyer, ev: EvalScope, fs: FileScope): Val ={
    val args = funct.f.params.allIndices.size>1
    var out = scala.collection.mutable.Map.empty[String, Val.Obj.Member]
    for((item, h) <- obj.getVisibleKeys()){
      out += (item -> Val.Obj.Member(add = false, Visibility.Normal, (_, _, _, _) => obj.value(item, -1)(fs, ev)))
    }
    if(args) {
      new Val.Obj(
        scala.collection.mutable.Map(
          out.toSeq.sortWith {
            case ((it1, h1), (it2, h2)) => (
              funct.apply(Val.Lazy(Val.Str(it1)), Val.Lazy(obj.value(it1, -1)(fs, ev))).toString >
                funct.apply(Val.Lazy(Val.Str(it2)), Val.Lazy(obj.value(it2, -1)(fs, ev))).toString
              )
          }: _*),
        _ => (), None)
    }
    else{
      new Val.Obj(
        scala.collection.mutable.Map(
          out.toSeq.sortWith {
            case ((it1, h1), (it2, h2)) => (
              funct.apply(Val.Lazy(Val.Str(it1))).toString >
                funct.apply(Val.Lazy(Val.Str(it2))).toString
              )
          }: _*),
        _ => (), None)
    }
  }

  def pluck(obj: Val.Obj, funct: Applyer, ev: EvalScope, fs: FileScope): Val ={
    val args= funct.f.params.allIndices.size
    val out = collection.mutable.Buffer.empty[Val.Lazy]
    args match {
      case 3 =>
        out.appendAll(obj.getVisibleKeys().zipWithIndex.map{
          case ((key, hidden), index) =>
            Val.Lazy(funct.apply(Val.Lazy(obj.value(key, -1)(fs, ev)), Val.Lazy(Val.Str(key)), Val.Lazy(Val.Num(index))))
        })
      case 2 =>
        out.appendAll(obj.getVisibleKeys().map{
          case (key, hidden) =>
            Val.Lazy(funct.apply(Val.Lazy(obj.value(key, -1)(fs, ev)), Val.Lazy(Val.Str(key))))
        })
      case 1 =>
        out.appendAll(obj.getVisibleKeys().map{
          case (key, hidden) =>
            Val.Lazy(funct.apply(Val.Lazy(obj.value(key, -1)(fs, ev))))
        })
    }
    Val.Arr(out.toSeq)
  }

  val libraries = Map(
    "Core" -> library(

      builtin("abs", "num"){
        (_,_, num: Double) =>
          Math.abs(num);
      },

      // TODO may have to change this since we need to validate that is an array of numbers
      // See: https://damieng.com/blog/2014/12/11/sequence-averages-in-scala
      // See: https://gist.github.com/gclaramunt/5710280
      builtin("avg", "array") {
        (_, _, array: Val.Arr) =>
          val (sum, length) = array.value.foldLeft((0.0, 0)) (
            {
              case ((sum, length), num) => (sum + num.force.asInstanceOf[Val.Num].value, 1 + length)
            }
          )
          sum / length
      },

      builtin("ceil", "num"){
        (_,_, num: Double) =>
          Math.ceil(num);
      },

      builtin("contains", "container", "value"){
        (_,_, container: Val, value: Val) =>
        container match{
          // See: scala.collection.IterableOnceOps.exists
          case Val.Arr(array) =>
              val evald = value
              var res = false
              val it = array.iterator
              while (!res && it.hasNext) {
                res = it.next().force == evald
              }
              res

            case Val.Str(s) =>
              val regex = value.cast[Val.Str].value.r
              regex.findAllMatchIn(s).toSeq.nonEmpty;
            case _ => throw new IllegalArgumentException(
              "Expected Array or String, got: " + container.prettyName);
          }
      },

      builtin("daysBetween", "datetime", "datetwo"){
        (_,_, datetimeone: String, datetimetwo: String) =>
          val dateone = java.time.ZonedDateTime
            .parse(datetimeone, DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSVV"))
          val datetwo = java.time.ZonedDateTime
            .parse(datetimetwo, DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSVV"))
          Val.Num(ChronoUnit.DAYS.between(dateone, datetwo)).value.abs;
      },

      builtin("distinctBy", "container", "funct"){
        (ev,fs, container: Val, funct: Applyer) =>
          container match {
            case Val.Arr(arr) =>
              distinctBy(arr,funct)
            case obj: Val.Obj =>
              distinctBy(obj,funct,ev,fs)
            case _ =>
              throw new IllegalArgumentException(
                "Expected Array or Object, got: " + container.prettyName);
          }
      },

      builtin("endsWith", "main", "sub"){
        (_,_, main: String, sub: String) =>
          main.toUpperCase.endsWith(sub.toUpperCase);
      },

      builtin("entriesOf", "obj"){
        (ev,fs, obj: Val.Obj) =>

          Val.Arr(obj.getVisibleKeys().toSeq.collect({
            case(key,hidden) =>
              val currentObj = scala.collection.mutable.Map[String, Val.Obj.Member]()
              currentObj += ("key" -> Val.Obj.Member(add =false, Visibility.Normal, (_, _, _, _) => Val.Lazy(Val.Str(key)).force))
              currentObj += ("value" -> Val.Obj.Member(add =false, Visibility.Normal, (_, _, _, _) => obj.value(key, -1)(fs, ev)))

              (Val.Lazy(new Val.Obj(currentObj, _ => (), None)))
          }))
      },

      builtin("filter", "array", "funct"){
        (_, _, value: Val, funct: Applyer) =>
          value match {
            case Val.Arr(array) =>
              filter(array, funct)
            case Val.Null => Val.Lazy(Val.Null).force
            case _ => throw new IllegalArgumentException(
              "Expected Array , got: " + value.prettyName);
          }
      },

      builtin("filterObject", "obj", "func"){
        (ev,fs, value: Val, func: Applyer) =>
          value match {
            case obj: Val.Obj =>
              filterObject(obj, func, ev, fs)
            case Val.Null => Val.Lazy(Val.Null).force
            case _ => throw new IllegalArgumentException(
              "Expected Object, got: " + value.prettyName);
          }
      },

      builtin("find", "container", "value"){
        (_,_, container: Val, value: Val) =>
          container match {
            case Val.Str(str) =>
              val out = collection.mutable.Buffer.empty[Val.Lazy]
              val sub = value.cast[Val.Str].value
              for(loc <- sub.r.findAllMatchIn(str).map(_.start)){
                  out.append(Val.Lazy(Val.Num(loc)))
              }
              Val.Arr(out.toSeq)
            case Val.Arr(s) =>
              Val.Arr(s.zipWithIndex.collect({
                case (v,i) if v.force == value => Val.Lazy(Val.Num(i))
              }))
            case _ => throw new IllegalArgumentException(
              "Expected Array or String, got: " + container.prettyName);
          }
      },

      builtin("flatMap", "array", "funct"){
        (_,_, array: Val, funct: Applyer) =>
          array match {
            case Val.Arr(s) =>
              flatMap(s,funct)
            case Val.Null => Val.Lazy(Val.Null).force
            case _ =>  throw new IllegalArgumentException(
              "Expected Array, got: " + array.prettyName);
          }
      },

      builtin("flatten", "array"){
        (_,_, array: Val) =>
          array match {
            case Val.Arr(outerArray)  =>
              val out = collection.mutable.Buffer.empty[Val.Lazy]
              for (innerArray <- outerArray) {
                innerArray.force match {
                  case Val.Null => out.append(Val.Lazy(Val.Null))
                  case Val.Arr(v) => out.appendAll(v)
                  case _ => throw new IllegalArgumentException (
                    "Expected Array, got: " + innerArray.force.prettyName);
                }
              }
              Val.Arr(out.toSeq)
            case Val.Null => Val.Lazy(Val.Null).force
            case _ => throw new IllegalArgumentException (
              "Expected Array, got: " + array.prettyName);
          }
      },

      builtin("floor", "num"){
        (_,_, num: Double) =>
          Math.floor(num);
      },

      builtin("groupBy", "container", "funct"){
        (ev,fs, container: Val, funct: Applyer) =>
          container match{
            case Val.Arr(s) =>
              groupBy(s, funct)
            case obj: Val.Obj =>
              groupBy(obj, funct, ev, fs)
            case Val.Null => Val.Lazy(Val.Null).force
            case _ => throw new IllegalArgumentException(
              "Expected Array or Object, got: " + container.prettyName);
          }
      },

      builtin("isBlank", "value"){
        (_,_, value: Val) =>
          value match {
            case Val.Str(s) => s.trim().isEmpty
            case Val.Null => true
            case _ => throw new IllegalArgumentException(
              "Expected String, got: " + value.prettyName);
          }
      },

      builtin("isDecimal", "value"){
        (_,_, value: Double) =>
          (Math.ceil(value) != Math.floor(value)).booleanValue()
      },

      builtin("isEmpty", "container"){
        (_,_, container: Val) =>
          container match{
            case Val.Null => true
            case Val.Str(s) => s.isEmpty.booleanValue()
            case Val.Arr(s) => s.isEmpty.booleanValue()
            case s: Val.Obj => s.getVisibleKeys().isEmpty.booleanValue()
            case _ => throw new IllegalArgumentException(
              "Expected String, Array, or Object, got: " + container.prettyName);
          }
      },

      builtin("isEven", "num"){
        (_, _, num: Double) =>
          (num % 2) == 0
      },

      builtin("isInteger", "value") {
        (_,_, value: Double) =>
          (Math.ceil(value) == Math.floor(value)).booleanValue()
      },

      builtin("isLeapYear", "datetime"){
        (_,_, datetime: String) =>
          java.time.ZonedDateTime
              .parse(datetime, DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSVV"))
                .toLocalDate.isLeapYear;
      },

      builtin("isOdd", "num"){
        (_, _, num: Double) =>
          (num % 2) != 0
      },

      builtin("joinBy", "array", "sep"){
        (_,_, array: Val.Arr, sep: String) =>
          var acc = ""
          for(x <- array.value){
            val value = x.force match{
              case Val.Str(x) => x
              case Val.True => "true"
              case Val.False => "false"
              case Val.Num(x) => if(Math.ceil(x) != Math.floor(x)) x.toString else x.intValue().toString
              case _ => throw new IllegalArgumentException(
                "Expected String, Number, Boolean, got: " + x.force.prettyName);
            }
            if(acc.isEmpty) acc += value else acc += (sep + value)
          }
          acc
      },

      builtin("keysOf", "obj"){
        (_,_, obj: Val.Obj) =>
          Val.Arr(
            obj.getVisibleKeys()
              .collect{case (k, _) => Val.Lazy(Val.Str(k))}.toSeq
          )
      },

      builtin("lower", "str"){
        (_,_, str: String) =>
          str.toLowerCase();
      },

      builtin("map", "array", "funct"){
        (_, _, array: Val, funct: Applyer) =>
          array match {
            case Val.Arr(seq) =>
              map(seq, funct)
            case Val.Null => Val.Lazy(Val.Null).force
            case _ => throw new IllegalArgumentException(
              "Expected Array, got: " + array.prettyName);
          }
      },

      builtin("mapObject", "value", "funct"){
        (ev,fs, value: Val, funct: Applyer) =>
          value match{
            case obj: Val.Obj =>
              mapObject(obj, funct, ev, fs)
            case Val.Null => Val.Lazy(Val.Null).force
            case _ =>  throw new IllegalArgumentException(
              "Expected Object, got: " + value.prettyName);
          }
      },

      builtin("match", "string", "regex") {
        (_, _, string: String, regex: String) =>
            val out=collection.mutable.Buffer.empty[Val.Lazy]
            for( word <- regex.r.findAllMatchIn(string)){
              for(index <- 0 to word.groupCount){
                out+=Val.Lazy(Val.Str(word.group(index)))
              }
            }
          Val.Arr(out.toSeq)
      },

      builtin("matches", "string", "regex") {
        (_,_, string: String, regex: String) =>
            regex.r.matches(string);
      },

      builtin("max", "array"){
        (_,_, array: Val.Arr) =>
          var value = array.value.head
          for (x <- array.value) {
            value.force.prettyName match {
              case "string" =>
                if (value.force.cast[Val.Str].value < x.force.cast[Val.Str].value) {
                  value = x
                }
              case "boolean" =>
                if (x.force == Val.Lazy(Val.True).force) {
                  value = x
                }
              case "number" =>
                if (value.force.cast[Val.Num].value < x.force.cast[Val.Num].value) {
                  value = x
                }
              case i => throw new IllegalArgumentException(
                "Array must be of type string,boolean, or number; got: " + i);
            }
          }
          value.force
      },

      builtin("maxBy", "array", "funct"){
        (_,_, array: Val.Arr, funct: Applyer) =>
          var value = array.value.head
          val compareType=funct.apply(value).prettyName
          for (x <- array.value) {
            compareType match {
              case "string" =>
                if (funct.apply(value).toString < funct.apply(x).toString) {
                  value = x
                }
              case "boolean" =>
                if (funct.apply(x) == Val.Lazy(Val.True).force) {
                  value = x
                }
              case "number" =>
                if (funct.apply(value).cast[Val.Num].value < funct.apply(x).cast[Val.Num].value) {
                  value = x
                }
              case i => throw new IllegalArgumentException(
                "Array must be of type string,boolean, or number; got: " + i);
            }
          }
          value.force
      },

      builtin("min", "array"){
        (_,_, array: Val.Arr) =>
          var value = array.value.head
          for (x <- array.value) {
            value.force.prettyName match {
              case "string" =>
                if (value.force.cast[Val.Str].value > x.force.cast[Val.Str].value) {
                  value = x
                }
              case "boolean" =>
                if (x.force == Val.Lazy(Val.False).force) {
                  value = x
                }
              case "number" =>
                if (value.force.cast[Val.Num].value > x.force.cast[Val.Num].value) {
                  value = x
                }
              case i => throw new IllegalArgumentException(
                "Array must be of type string,boolean, or number; got: " + i);
            }
          }
          value.force
      },

      builtin("minBy", "array", "funct"){
        (_,_, array: Val.Arr, funct: Applyer) =>
          var value = array.value.head
          val compareType=funct.apply(value).prettyName
          for (x <- array.value) {
            compareType match {
              case "string" =>
                if (funct.apply(value).cast[Val.Str].value > funct.apply(x).cast[Val.Str].value) {
                  value = x
                }
              case "boolean" =>
                if (funct.apply(x) == Val.Lazy(Val.False).force) {
                  value = x
                }
              case "number" =>
                if (funct.apply(value).cast[Val.Num].value > funct.apply(x).cast[Val.Num].value) {
                  value = x
                }
              case i => throw new IllegalArgumentException(
                "Array must be of type string,boolean, or number; got: " + i);
            }
          }
          value.force
      },

      builtin("mod", "num1", "num2"){
        (_,_, num1: Double, num2: Double) =>
          num1 % num2;
      },

      builtin("namesOf", "obj"){
        (_,_, obj: Val.Obj) =>
          Val.Arr(
            obj.getVisibleKeys()
              .collect{case (k, _) => Val.Lazy(Val.Str(k))}.toSeq
          )
      },

      builtin("orderBy", "value", "funct"){
        (ev,fs, value: Val, funct: Applyer) =>
          value match{
            case Val.Arr(array) =>
              orderBy(array, funct)
            case obj: Val.Obj =>
              orderBy(obj,funct,ev,fs)
            case Val.Null => Val.Lazy(Val.Null).force
            case _ => throw new IllegalArgumentException(
              "Expected Array or Object got: " + value.prettyName);
          }
      },

      builtin("pluck", "value", "funct"){
        (ev,fs, value: Val, funct: Applyer) =>
          value match{
            case obj: Val.Obj =>
              pluck(obj,funct,ev,fs)
            case Val.Null => Val.Lazy(Val.Null).force
            case _ => throw new IllegalArgumentException(
              "Expected Object, got: " + value.prettyName);
          }
      },

      builtin("pow", "num1", "num2"){
        (_,_, num1: Double, num2: Double) =>
          Math.pow(num1,num2)
      },

      builtin0("random") {
        (_, _,_) =>
          (0.0 + (1.0 - 0.0) * Random.nextDouble()).doubleValue()
      },

      builtin("randomint", "num") {
        (_,_, num: Int) =>
          (Random.nextInt((num - 0) + 1) + 0).intValue()
      },

      //TODO add read mediatype
      builtin("readUrl", "url"){
        (_,_, url: String) =>

          val out = new Scanner(new URL(url).openStream(), "UTF-8").useDelimiter("\\A").next()
          Materializer.reverse(UjsonUtil.jsonObjectValueOf(out));
      },

      //TODO needs work to get default from function
      builtin("reduce", "array", "funct", "init"){
        (_,_, array: Val.Arr, funct: Applyer, init: Val) =>
          var acc: Val = init;
          for(item <- array.value){
            acc=funct.apply(item,Val.Lazy(acc));
          }
          acc
      },

      builtin("replace", "string", "regex", "replacement") {
        (_,_, str: String, reg: String, replacement: String) =>
          reg.r.replaceAllIn(str,replacement)
      },

      builtin("round", "num") {
        (_,_, num: Double) =>
          Math.round(num).intValue()
      },

      builtin("scan", "str", "regex"){
        (_,_, str: String, regex: String) =>
          val out = collection.mutable.Buffer.empty[Val.Lazy]

          for(item <- regex.r.findAllMatchIn(str)){
            val subOut = collection.mutable.Buffer.empty[Val.Lazy]
            for(index <- 0 to item.groupCount){
              subOut+=Val.Lazy(Val.Str(item.group(index)))
            }
            out.append(Val.Lazy(Val.Arr(subOut.toSeq)))
          }
          Val.Arr(out.toSeq);
      },

      builtin("sizeOf", "value"){
        (_,_, value: Val) =>
          value match {
            case Val.Str(s) => s.length()
            case s: Val.Obj => s.getVisibleKeys().size
            case Val.Arr(s) => s.size
            case s: Val.Func => s.params.allIndices.size
            case Val.Null => 0
            case _ => throw new IllegalArgumentException(
              "Expected Array, String, Object got: " + value.prettyName);
          }
      },

      builtin("splitBy", "str", "regex"){
        (_,_, str: String, regex: String) =>
          val out = collection.mutable.Buffer.empty[Val.Lazy];
          for(words <- regex.r.split(str)){
            out+=Val.Lazy(Val.Str(words))
          }
          Val.Arr(out.toSeq)
      },

      builtin("sqrt", "num"){
        (_,_, num: Double) =>
          Math.sqrt(num)
      },

      builtin("startsWith", "str1", "str2"){
        (_,_, str1: String, str2: String) =>
          str1.toUpperCase().startsWith(str2.toUpperCase());
      },

      builtin("sum", "array"){
        (_,_, array: Val.Arr) =>
          var total = 0.0
          for(x <- array.value){
            x.force match {
              case Val.Num(s) => total += s
              case _ => throw new IllegalArgumentException(
                "Expected Array of numbers got: " + x.force.prettyName);
            }
          }
          total
      },

      builtin("to", "begin", "end"){
        (_,_, begin: Int, end: Int) =>
          Val.Arr(
            (begin to end).map( i =>
              Val.Lazy(Val.Num(i))
            )
          )
      },

      builtin("trim", "str"){
        (_,_, str: String) =>
          str.trim()
      },

      builtin("typeOf", "value"){
        (_,_, value: Val) =>
          value match{
            case Val.True | Val.False => "boolean"
            case Val.Null => "null"
            case _: Val.Obj => "object"
            case _: Val.Arr => "array"
            case _: Val.Func => "function"
            case _: Val.Num => "number"
            case _: Val.Str => "string"
          }
      },

      builtin("unzip", "array"){
        (_, _, array: Val.Arr) =>
          var size = Int.MaxValue
          for(v <- array.value){
            v.force match{
              case Val.Arr(s) => if(s.size < size) size = s.size
              case _ =>   throw new IllegalArgumentException(
                "Expected Array, got: " + v.force.prettyName);
            }
          }
          val out = collection.mutable.Buffer.empty[Val.Lazy]
          for(i <-0 until size) {
            val current = collection.mutable.Buffer.empty[Val.Lazy]
            for (x <- array.value) {
              x.force match{
                case Val.Arr(s) => current.append(s(i))
                case _ => throw new IllegalArgumentException(
                  "Expected Array, got: " + x.force.prettyName);
              }
            }
            out.append(Val.Lazy(Val.Arr(current.toSeq)))
          }
          Val.Arr(out.toSeq)
      },

      builtin("upper", "str"){
        (_,_, str: String) =>
          str.toUpperCase()
      },

      builtin0("uuid") {
        (_, _, _) =>
          Materializer.reverse(UjsonUtil.jsonObjectValueOf(com.datasonnet.DWCore.uuid()));
      },

      builtin("valuesOf", "obj"){
        (ev,fs, obj: Val.Obj) =>
          val out = collection.mutable.Buffer.empty[Val.Lazy]
          for((key,hidden) <- obj.getVisibleKeys()){
            out.append(Val.Lazy(obj.value(key, -1)(fs, ev)))
          }
          Val.Arr(out.toSeq)
      },

      builtin("zip", "array1", "array2") {
        (_, _, array1: Val.Arr, array2: Val.Arr) =>

          val smallArray = if(array1.value.size <= array2.value.size) array1 else array2
          val bigArray = (if(smallArray == array1) array2 else array1).value
          val out = collection.mutable.Buffer.empty[Val.Lazy]
          for((v,i) <- smallArray.value.zipWithIndex){
            val current = collection.mutable.Buffer.empty[Val.Lazy]
            if(smallArray == array1) {
              current.append(v)
              current.append(bigArray(i))
            }
            else{
              current.append(bigArray(i))
              current.append(v)
            }
            out.append(Val.Lazy(Val.Arr(current.toSeq)))
          }
          Val.Arr(out.toSeq)
      }
    ),
    "Crypto" -> library(
      //TODO
      builtin("HMACBinary", "bin1", "bin2", "str"){
        (ev,fs, bin1: Val, bin2: Val, str: String) =>
          Val.Lazy(Val.Null).force
      },
      //TODO
      builtin("HMACWith", "bin1", "bin2", "str"){
        (ev,fs, bin1: Val, bin2: Val, str: String) =>
          Val.Lazy(Val.Null).force
      },
      //TODO - EWN Test 
      builtin("MD5", "str"){
        (ev,fs, str: String) =>
          datasonnet.Crypto.hash(str,"MD5");
      },
      //TODO
      builtin("SHA1", "str"){
        (ev,fs, str: String) =>
          Val.Lazy(Val.Null).force
      },
      //TODO
      builtin("hashWith", "bin", "str"){
        (ev,fs, bin: Val, str: String) =>
          Val.Lazy(Val.Null).force
      },
    ),
    "Arrays" -> library(
      builtin("countBy", "arr", "funct"){
        (ev,fs, arr: Val.Arr, funct: Applyer) =>
          var total = 0
          for(x <- arr.value){
            if(funct.apply(x) == Val.True){
              total+=1
            }
          }
          total
      },
      //TODO
      builtin("divideBy", "arr", "num"){
        (ev,fs, arr: Val.Arr, num: Double) =>
          Val.Lazy(Val.Null).force
      },
      //TODO
      builtin("drop", "arr", "num"){
        (ev,fs, arr: Val.Arr, num: Double) =>
          Val.Lazy(Val.Null).force
      },
      //TODO
      builtin("dropWhile", "arr", "funct"){
        (ev,fs, arr: Val.Arr, funct: Applyer) =>
          Val.Lazy(Val.Null).force
      },
      //TODO
      builtin("every", "arr", "funct"){
        (ev,fs, arr: Val.Arr, funct: Applyer) =>
          Val.Lazy(Val.Null).force
      },
      //TODO
      builtin("firstWith", "arr", "funct"){
        (ev,fs, arr: Val.Arr, funct: Applyer) =>
          Val.Lazy(Val.Null).force
      },
      //TODO
      builtin("indexOf", "value"){
        (ev,fs, value: Val) =>
          Val.Lazy(Val.Null).force
      },
      //TODO
      builtin("indexWhere", "arr", "funct"){
        (ev,fs, arr: Val.Arr, funct: Applyer) =>
          Val.Lazy(Val.Null).force
      },
      //TODO
      builtin("join", "arr", "funct"){
        (ev,fs, arr: Val.Arr, funct: Applyer) =>
          Val.Lazy(Val.Null).force
      },
      //TODO
      builtin("leftJoin", "arr", "funct"){
        (ev,fs, arr: Val.Arr, funct: Applyer) =>
          Val.Lazy(Val.Null).force
      },
      //TODO
      builtin("outerJoin", "arr", "funct"){
        (ev,fs, arr: Val.Arr, funct: Applyer) =>
          Val.Lazy(Val.Null).force
      },
      //TODO
      builtin("partition", "arr", "funct"){
        (ev,fs, arr: Val.Arr, funct: Applyer) =>
          Val.Lazy(Val.Null).force
      },
      //TODO
      builtin("slice", "arr", "funct"){
        (ev,fs, arr: Val.Arr, funct: Applyer) =>
          Val.Lazy(Val.Null).force
      },
      //TODO
      builtin("some", "arr", "funct"){
        (ev,fs, arr: Val.Arr, funct: Applyer) =>
          Val.Lazy(Val.Null).force
      },
      //TODO
      builtin("splitAt", "arr", "funct"){
        (ev,fs, arr: Val.Arr, funct: Applyer) =>
          Val.Lazy(Val.Null).force
      },
      //TODO
      builtin("splitWhere", "arr", "funct"){
        (ev,fs, arr: Val.Arr, funct: Applyer) =>
          Val.Lazy(Val.Null).force
      },
      //TODO
      builtin("sumBy", "arr", "funct"){
        (ev,fs, arr: Val.Arr, funct: Applyer) =>
          Val.Lazy(Val.Null).force
      },
      //TODO
      builtin("take", "arr", "funct"){
        (ev,fs, arr: Val.Arr, funct: Applyer) =>
          Val.Lazy(Val.Null).force
      },
      //TODO
      builtin("takeWhile", "arr", "funct"){
        (ev,fs, arr: Val.Arr, funct: Applyer) =>
          Val.Lazy(Val.Null).force
      }
    ),
    "Binaries" -> library(
      //TODO
      builtin("fromBase64", "value"){
        (ev,fs, value: Val) =>
          Val.Lazy(Val.Null).force
      },
      //TODO
      builtin("fromHex", "value"){
        (ev,fs, value: Val) =>
          Val.Lazy(Val.Null).force
      },
      //TODO
      builtin("readLinesWith", "value"){
        (ev,fs, value: Val) =>
          Val.Lazy(Val.Null).force
      },
      //TODO
      builtin("toBase64", "value"){
        (ev,fs, value: Val) =>
          Val.Lazy(Val.Null).force
      },
      //TODO
      builtin("toHex", "value"){
        (ev,fs, value: Val) =>
          Val.Lazy(Val.Null).force
      },
      //TODO
      builtin("writeLinesWith", "value"){
        (ev,fs, value: Val) =>
          Val.Lazy(Val.Null).force
      },
    ),
    "Numbers" -> library(
      //TODO
      builtin("fromBinary", "value"){
        (ev,fs, value: Val) =>
          Val.Lazy(Val.Null).force
      },
      //TODO
      builtin("fromHex", "value"){
        (ev,fs, value: Val) =>
          Val.Lazy(Val.Null).force
      },
      //TODO
      builtin("fromRadixNumber", "value"){
        (ev,fs, value: Val) =>
          Val.Lazy(Val.Null).force
      },
      //TODO
      builtin("toBinary", "value"){
        (ev,fs, value: Val) =>
          Val.Lazy(Val.Null).force
      },
      //TODO
      builtin("toHex", "value"){
        (ev,fs, value: Val) =>
          Val.Lazy(Val.Null).force
      },
      //TODO
      builtin("toRadixNumber", "value"){
        (ev,fs, value: Val) =>
          Val.Lazy(Val.Null).force
      },
    ),
    "Objects" -> library(
      //TODO
      builtin("divideBy", "value"){
        (ev,fs, value: Val) =>
          Val.Lazy(Val.Null).force
      },
      //TODO
      builtin("entrySet", "value"){
        (ev,fs, value: Val) =>
          Val.Lazy(Val.Null).force
      },
      //TODO
      builtin("everyEntry", "value"){
        (ev,fs, value: Val) =>
          Val.Lazy(Val.Null).force
      },
      //TODO
      builtin("keySet", "value"){
        (ev,fs, value: Val) =>
          Val.Lazy(Val.Null).force
      },
      //TODO
      builtin("mergeWith", "value"){
        (ev,fs, value: Val) =>
          Val.Lazy(Val.Null).force
      },
      //TODO
      builtin("nameSet", "value"){
        (ev,fs, value: Val) =>
          Val.Lazy(Val.Null).force
      },
      //TODO
      builtin("someEntry", "value"){
        (ev,fs, value: Val) =>
          Val.Lazy(Val.Null).force
      },
      //TODO
      builtin("someEntry", "value"){
        (ev,fs, value: Val) =>
          Val.Lazy(Val.Null).force
      },
      //TODO
      builtin("takeWhile", "value"){
        (ev,fs, value: Val) =>
          Val.Lazy(Val.Null).force
      },
      //TODO
      builtin("valueSet", "value"){
        (ev,fs, value: Val) =>
          Val.Lazy(Val.Null).force
      },
    ),
    "Strings" -> library(
      builtin("appendIfMissing", "str1", "str2"){
        (ev,fs, str: String, append: String) =>
          var ret = str;
          if(!str.contains(append)){
            ret=(str + append)
          }
          Val.Lazy(Val.Str(ret)).force
      },
      builtin("camelize", "str"){
        (ev,fs, str: Val) =>
          str match{
            case Val.Str(value) =>
              //regex fo _CHAR
              var regex = "(_+)([0-9A-Za-z])".r("underscore", "letter")

              //Start string at first non underscore, lower case it
              var temp = value.substring("[^_]".r.findFirstMatchIn(value).map(_.start).toList.head )
              temp = temp.replaceFirst(temp.charAt(0).toString, temp.charAt(0).toLower.toString)

              //replace and uppercase
              temp = regex.replaceAllIn(temp, m => s"${(m group "letter").toUpperCase()}");
              Val.Lazy(Val.Str(temp)).force;

            case Val.Null =>
              Val.Lazy(Val.Null).force
            case _ => throw new IllegalArgumentException(
              "Expected String got: " + str.prettyName);
          }
      },
      //TODO
      builtin("capitalize", "str"){
        (ev,fs, str: Val) =>
          Val.Lazy(Val.Null).force
      },
      builtin("charCode", "str"){
        (ev,fs, str: String) =>
          str.codePointAt(0)
      },
      builtin("charCodeAt", "str", "num"){
        (ev,fs, str: String, num: Int) =>
          str.codePointAt(num)

      },
      //TODO
      builtin("dasherize", "value"){
        (ev,fs, value: Val) =>
          Val.Lazy(Val.Null).force
      },
      builtin("fromCharCode", "num"){
        (ev,fs, num: Int) =>
          String.valueOf(num.asInstanceOf[Char])
      },

      builtin("isAlpha", "str"){
        (_,_, str: Val) =>
          str match {
            case Val.Str(value) =>
              if("^[A-Za-z]+$".r.matches(value)) {true}
              else {false}
            case Val.Null => false
            case Val.Num(x) => false
            case Val.True | Val.False => true
            case _ => throw new IllegalArgumentException(
              "Expected String, got: " + str.prettyName);
          }
      },

      builtin("isAlphanumeric", "str"){
        (ev,fs, str: Val) =>
          str match {
            case Val.Str(value) =>
              if("^[A-Za-z0-9]+$".r.matches(value)) {true}
              else {false}
            case Val.Null => false
            case Val.Num(x) => true
            case Val.True | Val.False => true
            case _ => throw new IllegalArgumentException(
              "Expected String, got: " + str.prettyName);
          }
      },

      builtin("isLowerCase", "str"){
        (ev,fs, str: Val) =>
          str match {
            case Val.Str(value) =>
              if("^[a-z]+$".r.matches(value)) {true}
              else {false}
            case Val.Null => false
            case Val.Num(x) => false
            case Val.True | Val.False => true
            case _ => throw new IllegalArgumentException(
              "Expected String, got: " + str.prettyName);
          }
      },
      //TODO
      builtin("isNumeric", "str"){
        (ev,fs, str: Val) =>
          str match {
            case Val.Str(value) =>
              if("^[0-9]+$".r.matches(value)) {true}
              else {false}
            case Val.Null => false
            case Val.Num(x) => true
            case Val.True | Val.False => false
            case _ => throw new IllegalArgumentException(
              "Expected String, got: " + str.prettyName);
          }
      },
      //TODO
      builtin("isUpperCase", "value"){
        (ev,fs, value: Val) =>
          Val.Lazy(Val.Null).force
      },
      //TODO
      builtin("isWhitespace", "value"){
        (ev,fs, value: Val) =>
          Val.Lazy(Val.Null).force
      },
      //TODO
      builtin("leftPad", "value"){
        (ev,fs, value: Val) =>
          Val.Lazy(Val.Null).force
      },
      //TODO
      builtin("ordinalize", "value"){
        (ev,fs, value: Val) =>
          Val.Lazy(Val.Null).force
      },
      //TODO
      builtin("pluralize", "value"){
        (ev,fs, value: Val) =>
          Val.Lazy(Val.Null).force
      },
      //TODO
      builtin("prependifMissing", "value"){
        (ev,fs, value: Val) =>
          Val.Lazy(Val.Null).force
      },
      //TODO
      builtin("repeat", "value"){
        (ev,fs, value: Val) =>
          Val.Lazy(Val.Null).force
      },
      //TODO
      builtin("rightPad", "value"){
        (ev,fs, value: Val) =>
          Val.Lazy(Val.Null).force
      },
      //TODO
      builtin("singularize", "value"){
        (ev,fs, value: Val) =>
          Val.Lazy(Val.Null).force
      },
      //TODO
      builtin("substringAfter", "value"){
        (ev,fs, value: Val) =>
          Val.Lazy(Val.Null).force
      },
      //TODO
      builtin("substringAfterLast", "value"){
        (ev,fs, value: Val) =>
          Val.Lazy(Val.Null).force
      },
      //TODO
      builtin("substringBefore", "value"){
        (ev,fs, value: Val) =>
          Val.Lazy(Val.Null).force
      },
      //TODO
      builtin("substringBeforeLast", "value"){
        (ev,fs, value: Val) =>
          Val.Lazy(Val.Null).force
      },
      //TODO
      builtin("underscore", "value"){
        (ev,fs, value: Val) =>
          Val.Lazy(Val.Null).force
      },
      //TODO
      builtin("unwrap", "value"){
        (ev,fs, value: Val) =>
          Val.Lazy(Val.Null).force
      },
      //TODO
      builtin("withMaxSize", "value"){
        (ev,fs, value: Val) =>
          Val.Lazy(Val.Null).force
      },
      //TODO
      builtin("wrapifMissing", "value"){
        (ev,fs, value: Val) =>
          Val.Lazy(Val.Null).force
      },
      //TODO
      builtin("wrapWith", "value"){
        (ev,fs, value: Val) =>
          Val.Lazy(Val.Null).force
      },
    )
  )
}
