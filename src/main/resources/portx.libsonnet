{
    local PortXMapper = self,

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
      * Example:
      *
      *   filter(input.languages, 'language', 'Java')
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
      *         "name": "Foo",
      *         "language": "Java"
      *       },
      *       {
      *         "name": "FooBar",
      *         "language": "Java"
      *       }
      *    ]
      */
    filter(listOfObjects, key, value)::
          [ obj for obj in listOfObjects if PortXMapper.select(obj, key) == value ],

    local keys(arr, key) = std.uniq(std.sort([ PortXMapper.select(obj, key) for obj in arr ])),

    /*
     * Partitions an array into a Object that contains Arrays, according to the discriminator lambda you define
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
    groupBy(arr, keyName):: {
        [key]: PortXMapper.filter(arr, keyName, key)
            for key in keys(arr, keyName)
    },

    /*
     * Removes a property with given name from the object and returns the remaining object
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
    }
}