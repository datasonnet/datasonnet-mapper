/**
 * Copyright 2019-2020 the original author or authors.
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
{
    local PortXMapper = self,
    local defaultF = function(x) x,

    /*
     * DEPRECATED Returns a value inside the object by given path separated by dot ('.')
     *
     * Example:
     *
     *   select(input, 'language.name')
     *
     *   Input:
     *   {
     *      "name": "Foo",
     *      "language": {
     *          "name": "Java",
     *          "version": "1.8"
     *      }
     *   }
     *
     *   Output:
     *   "Java"
     */
    select(object, path)::
      local arr = std.split(path, '.');
      if std.length(arr) > 1 then
        PortXMapper.select(object[arr[0]], std.join('.', arr[1:]))
      else
        object[arr[0]],

     /*
      * DEPRECATED Filters array of objects by given condition.
      *
      *   filterEx(input.languages, 'language', 'Java', function(x, y) x != y)
      *
      *   Input:
      *   {
      *     "languages": [
      *       {
      *         "name": "Foo",
      *         "language": "Java"
      *       },
      *       {
      *         "name": "Bar",
      *         "language": "Scala"
      *       },
      *       {
      *         "name": "FooBar",
      *         "language": "Java"
      *       }
      *     ]
      *   }
      *
      *   Output:
      *   [
      *       {
      *         "name": "Bar",
      *         "language": "Scala"
      *       }
      *    ]
      */
    filterEx(listOfObjects, key, value, filter_func=function(value1, value2) value1 == value2)::
          [ obj for obj in listOfObjects if filter_func(PortXMapper.select(obj, key), value) ],

    /*
     * DEPRECATED Partitions an array into a Object that contains Arrays, according to the discriminator lambda you define
     * The discriminator can be a path inside the objects to group, e.g. 'language.name'
     *
     * Example:
     *    groupBy(payload.languages, 'language.name')
     *
     *    Input:
     *    {
     *      "languages": [
     *        {
     *          "name": "Foo",
     *          "language": {
     *              "name": "Java",
     *              "version": "1.8"
     *          }
     *        },
     *        {
     *          "name": "Bar",
     *          "language": {
     *              "name": "Scala",
     *              "version": "1.0"
     *          }
     *        },
     *        {
     *          "name": "FooBar",
     *          "language": {
     *              "name": "Java",
     *              "version": "1.7"
     *          }
     *        }
     *      ]
     *    }
     *
     *    Output:
     *    {
     *       "languages": {
     *          "Java": [
     *             {
     *                "language": {
     *                   "name": "Java",
     *                   "version": "1.8"
     *                },
     *                "name": "Foo"
     *             },
     *             {
     *                "language": {
     *                   "name": "Java",
     *                   "version": "1.7"
     *                },
     *                "name": "FooBar"
     *             }
     *          ],
     *          "Scala": [
     *             {
     *                "language": {
     *                   "name": "Scala",
     *                   "version": "1.0"
     *                },
     *                "name": "Bar"
     *             }
     *          ]
     *       }
     *    }
     */
    groupBy(arr, keyName)::
        local keys(arr, key) = std.uniq(std.sort([ PortXMapper.select(obj, key) for obj in arr ]));

        {
            [key]: PortXMapper.filterEx(arr, keyName, key)
                for key in keys(arr, keyName)
        },

    /*
     * DEPRECATED Removes a property with given name from the object and returns the remaining object
     *
     * Example:
     *
     *    remove(flight, 'availableSeats')
     *
     *    Input:
     *    {
     *      "availableSeats": 45,
     *      "airlineName": "Delta",
     *      "aircraftBrand": "Boeing",
     *      "aircraftType": "717",
     *      "departureDate": "01/20/2019",
     *      "origin": "PHX",
     *      "destination": "SEA"
     *    }
     *
     *    Output:
     *    {
     *      "airlineName": "Delta",
     *      "aircraftBrand": "Boeing",
     *      "aircraftType": "717",
     *      "departureDate": "01/20/2019",
     *      "origin": "PHX",
     *      "destination": "SEA"
     *    }
     *
     */
    remove(object, keyName):: {
      [ key ]: object[key]
         for key in std.objectFields(object)
         if key != keyName
    },

    /*
     * DEPRECATED Removes fields with names matching any of the array values from the object
     */
    removeAll(obj, arr)::
        local setOfKeys = std.set(arr);
        {
            [ if !std.setMember(k, setOfKeys) then k] : obj[k]
            for k in std.objectFieldsAll(obj)
        },
    /*
     * DEPRECATED Flattens multiple nested arrays into a single array
     *
     * Example:
     *
     *    flattenArrays(arr)
     *
     *    Input:
     *    [
     *       1,
     *       2,
     *       [
     *         3
     *       ],
     *       [
     *         4,
     *         [
     *           5,
     *           6,
     *           7
     *         ],
     *        {
     *           "x": "y"
     *         }
     *       ]
     *     ]
     *
     *    Output:
     *    [
     *        1,
     *        2,
     *        4,
     *        5,
     *        6,
     *        7,
     *        {
     *           "x": "y"
     *        }
     *     ]
     *
     */
    deepFlattenArrays(arr)::
        std.foldl(function(aggregate, x) if std.isArray(x) then aggregate + PortXMapper.deepFlattenArrays(x) else aggregate + [x], arr, []),

    /*
     * DEPRECATED Returns an array with elements in reverse order.
     *
     * Example:
     *
     *    reverse(arr)
     *
     *    Input:
     *    [
     *       "a",
     *       "b",
     *       "c",
     *       "d"
     *    ]
     *
     *    Output:
     *    [
     *       "d",
     *       "c",
     *       "b",
     *       "a",
     *    ]
     *
     */
    reverse(arr)::
        [
            arr[std.length(arr) - idx - 1]
            for idx in std.range(0, std.length(arr) - 1)
        ],

    /*
     * DEPRECATED Parses a string which contains a double number and returns its numeric representation
     */
    parseDouble(str)::
        local parts = std.split(str, ".");
        local intPart = std.parseInt(parts[0]);
        intPart + (if (std.length(parts) > 1) then std.parseInt(parts[1]) * std.pow(10, -std.length(parts[1])) else 0) * if (intPart < 0) then -1 else 1,

    /*
     * DEPRECATED Returns an array containing duplicate elements from input array
     */
    duplicates(arr, keyF=defaultF, set=true)::
        local aggregates = std.foldl(function(aggregate, x)
                                       aggregate + if (std.setMember(x, aggregate.found, keyF)) then
                                                   { duplicates: aggregate.duplicates + [x] } else
                                                   { found: std.set(aggregate.found + [x], keyF) },
                                       arr, {
                                              found: [],
                                              duplicates: []
                                            },
                                       );
        if set then std.set(aggregates.duplicates, keyF) else aggregates.duplicates,

    /*
     * DEPRECATED Returns sum of all elements in the array
     */
    sum(arr)::
        std.foldl(function(aggregate, num) aggregate + num, payload, 0),

    /*
     * DEPRECATED Rounds a double to the number of digits after the decimal point
     */
    round(num, precision)::
        assert std.isNumber(num) : "Argument 'num' must be a number, got " + std.type(num);
        assert std.isNumber(precision) &&
               precision >= 0 &&
               std.length(std.split(std.toString(precision), '.')) < 2 : "Argument 'precision' must be an integer number greater than or equal to 0";

        local shift = std.pow(10,precision);
        local shiftedNum = num * shift;
        local stringRep = std.format("%f", shiftedNum);
        local decimalParts = std.split(stringRep, ".");

        if std.length(decimalParts) < 2 then num else
            local nextDigit = self.parseDouble(decimalParts[1][0]);
            local rnd = if num >= 0 then
                           if (nextDigit >= 5) then std.ceil(shiftedNum) else std.floor(shiftedNum)
                        else
                           if (nextDigit >= 5) then std.floor(shiftedNum) else std.ceil(shiftedNum);
            local rounded = rnd / shift;
            self.parseDouble(std.format("%." + precision + "f", rounded)),

    /*
     * DEPRECATED Returns an object where keys are the results of calling keyF on the values, and the values are the counts of values that produced the corresponding key.
     */
    counts(arr, keyF=defaultF)::
        std.foldl(function(aggregate, x)
                    aggregate +
                        {
                            [keyF(x)] +: 1
                        },
                  arr,
                  {}),

    /*
     * Turns an array into an object, where the keys are the result of calling keyF on each value (which becomes the value at the key). If valueF is provided it gets run on the value.
     */
    mapToObject(arr, keyF, valueF=defaultF)::
        std.foldl(function(aggregate, x)
                     local keyX = keyF(x);
                     if (std.objectHas(aggregate, keyX)) then aggregate
                     else aggregate + { [keyX]: valueF(x) },
                  arr,
                  {}),
}