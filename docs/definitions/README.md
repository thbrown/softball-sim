# Optimizer Definition Documentation

This document describes the format of the definition files. Unit tests will validate that all definitions are formatted correctly, and meet the requirements. All fields are required unless otherwise noted. You can also have a look at the definition files in this directory for examples.

## Root Fields
- **id** - Number. This should match the id used in the file name.
- **name** - String. The name of the optimization, make this whatever you like.
- **img** - String. Url to the image to use in the gallery for this optimizer.
- **shortDescription** - String. A brief description of your optimizer, how it works, and what makes it unique from other optimizers. There is currently no hard length limit for this but try to keep it succinct.
- **longDescriptionFile** - String. The name of the file which serves as a README for the optimzer. In this file you can get into all the dirty details about how your optimizer works. The actual file should be added inside the ../descriptions directory.
- **supportedLineupTypes** - Array of Strings. The lineup types that this optimizer supports, optimizers don't have to support all lineup types, but you must support at least one! Valid values `NORMAL`, `ALTERNATING_GENDER`, and `NO_CONSECUTIVE_FEMALES`
- **options** - Array of Objects. these are the arguments that the optimizer consumes. If the optimizer has no options, set this to an empty array.

### Options Fields
- **type** - String. The data type of the field. Here are the supported types: `String`, `Number`, `Boolean`, `Enumeration`. Each of these values may add additional fields to the options object. See the "Additional Fields" sections below for more details.
- **shortLabel** - String. A single character to describe this option. Used as the short flag for this argument in the CLI. Must be uppercase. Since there are 26 letters in the English alphabet, this leaves you with a maximum of 26 possible options.
- **longLabel** - String. A single word (no spaces allowed) to describe this option. Used as the long flag for this argument in the CLI. Case-sensitive. 
- **description** - String. A short paragraph describing the purpose of this option, the tradeoffs of using various values, and any other pertinent information about how it should be used.
- **uiVissibility** - String. Not required. Defines whether or not this field will appear in the softball.app ui. Choices are `STANDARD` or `HIDDEN`. Defaults to `STANDARD`.

#### Additional Fields for Number
- **max** - Number. Not required. The maximum value allowed (inclusive). Application will throw an exception if user attempts to provide a value higher than max.
- **min** - Number. Not required. The minimum value allowed (inclusive). Application will throw an exception if user attempts to provide a value lower than min.
- **step** - Number. Not required. The legal number intervals. If a user specifies a value that is not divisible by step, the application will print a warning and round the user's input value to the nearest step. This rounding can cause the value to violate the min and max constraints and cause the application to throw an exception.
- **defaultValue** - String. Not required. The value that should be used if no value is specified.

#### Additional Fields for String
- **pattern** - String. Not required. A regular expression that the input must match to be valid.
- **defaultValue** - String. Not required. The value that should be used if no explicit value is specified.

#### Additional Fields for Enumeration
- **values** - Array of Strings. List of allowable values.
- **defaultValue** - String. Not required. The value that should be used if no explicit value is specified.

#### Additional Fields for Boolean
*no additional fields*

## Other Considerations

Occasionally you may wish to specify a default value that can't be known ahead of time. For example, the number of CPU cores that exist on the machine that will run the optimizer, some default based on the value of other options, or something else entirely. In these cases, consider omitting the defaultValue field and handling null in your implementation.

I don't have a use case for a 'required' field yet so it has not yet been implemented. If you need this, open a Github issue.