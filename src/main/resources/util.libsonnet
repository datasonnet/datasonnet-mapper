{
    local PortXMapper = self,

    local defaultF = function(x) x,
    /*
     * Returns a value inside the object by given path separated by dot ('.')
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
      * Filters array of objects by given condition.
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
     * Removes fields with names matching any of the array values from the object
     */
    removeAll(obj, arr)::
        local setOfKeys = std.set(arr);
        {
            [ if !std.setMember(k, setOfKeys) then k] : obj[k]
            for k in std.objectFieldsAll(obj)
        },
    /*
     * Flattens multiple nested arrays into a single array
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
     * Returns an array with elements in reverse order.
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
     * Returns an array containing duplicate elements from input array
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
     * Returns an object where keys are the results of calling keyF on the values, and the values are the counts of values that produced the corresponding key.
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