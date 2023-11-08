### Changelog

All notable changes to this project will be documented in this file. Dates are displayed in UTC.
#### [3.0.0-RC1](https://github.com/datasonnet/datasonnet-mapper/compare/2.5.2...3.0.0-RC1)

> 8 November 2023

- Debugger server added
- `ds.util` library removed as redundant
- Fully migrated from `javax` to `jakarta` JAXB implementation

#### [2.5.2](https://github.com/datasonnet/datasonnet-mapper/compare/2.5.1...2.5.2), [2.5.2-jakarta4](https://github.com/datasonnet/datasonnet-mapper/compare/2.5.1-jakarta...2.5.2-jakarta4)

> 28 April 2023

- Add optional `IV` parameter for `encrypt` and `decrypt` functions [`#119`](https://github.com/datasonnet/datasonnet-mapper/pull/119)

#### [2.5.1](https://github.com/datasonnet/datasonnet-mapper/compare/2.5...2.5.1), [2.5.1-jakarta](https://github.com/datasonnet/datasonnet-mapper/compare/2.5...2.5.1-jakarta)

> 13 March 2023

- Yaml plugin added [`#82`](https://github.com/datasonnet/datasonnet-mapper/pull/82)
- Unified support for Jakarta4 and Jakarta &lt; 4 or JavaX JAXB [`#116`](https://github.com/datasonnet/datasonnet-mapper/pull/116)
- `Period` library documentation [`5ef390d`](https://github.com/datasonnet/datasonnet-mapper/commit/5ef390df6e6b7220f6d1b1ea4b288188336a97aa)
- Add `OffsetDateTime` test [`ff47427`](https://github.com/datasonnet/datasonnet-mapper/commit/ff47427d115f7958954f2f037f5142a5decf2b04)
- Add `ds.datetime.daysBetween` function [`e8128db`](https://github.com/datasonnet/datasonnet-mapper/commit/e8128db17e2e015debf594543b24df0f8e34495f)

#### [2.5](https://github.com/datasonnet/datasonnet-mapper/compare/2.2.0...2.5)

> 18 January 2023

- Add missing `Std `functions from Jsonnet 0.20 spec [`#115`](https://github.com/datasonnet/datasonnet-mapper/pull/115)
- `Std.get()` implemented (addresses issue #110) [`#114`](https://github.com/datasonnet/datasonnet-mapper/pull/114)
- Jsonnet engine merged into datasonnet [`#109`](https://github.com/datasonnet/datasonnet-mapper/pull/109)
- `Std.all()` [`b6ae1d6`](https://github.com/datasonnet/datasonnet-mapper/commit/b6ae1d634f3f352ff62615dfc61e4e1f032c92da)

#### [2.2.0](https://github.com/datasonnet/datasonnet-mapper/compare/2.1.4...2.2.0)

> 16 November 2022

- Multipart plugin [`#106`](https://github.com/datasonnet/datasonnet-mapper/pull/106)
- Added `localdatetime` module in lower case `ds` module. [`#101`](https://github.com/datasonnet/datasonnet-mapper/pull/101)
- Address the issue #102 [`#103`](https://github.com/datasonnet/datasonnet-mapper/pull/103)
- UUID generation fix (only hexadecimal characters allowed) [`#95`](https://github.com/datasonnet/datasonnet-mapper/pull/95)

#### [2.1.4](https://github.com/datasonnet/datasonnet-mapper/compare/2.1.3...2.1.4)

> 2 July 2021

- update repository information in project [`#94`](https://github.com/datasonnet/datasonnet-mapper/pull/94)
- MixIns and polymorphic deserialization support [`#93`](https://github.com/datasonnet/datasonnet-mapper/pull/93)
- Unix timestamp [`#86`](https://github.com/datasonnet/datasonnet-mapper/pull/86)
- Added simplified polymorphism support [`ac6b410`](https://github.com/datasonnet/datasonnet-mapper/commit/ac6b4104b4469b4efe6ef91a6166a53ae2ae59c0)
- change to using a non-reflection approach for polymorphic types [`b07b4a1`](https://github.com/datasonnet/datasonnet-mapper/commit/b07b4a157be8c0bd0b3492548b0b90512bb534eb)

#### [2.1.3](https://github.com/datasonnet/datasonnet-mapper/compare/2.1.2...2.1.3)

> 10 May 2021

- conform writer targettype support in csv and json plugins [`5d219c4`](https://github.com/datasonnet/datasonnet-mapper/commit/5d219c4c6ec16b839ed489b713a277f1b49cd95c)

#### [2.1.2](https://github.com/datasonnet/datasonnet-mapper/compare/2.1.1...2.1.2)

> 25 February 2021

- adds write null support for default java plugin [`17ae3b5`](https://github.com/datasonnet/datasonnet-mapper/commit/17ae3b55814c877bc6bb43ae6f7c818cb12d5b1f)

#### [2.1.1](https://github.com/datasonnet/datasonnet-mapper/compare/2.1.0...2.1.1)

> 4 December 2020

- adds writers mediatype coverage [`afca0e5`](https://github.com/datasonnet/datasonnet-mapper/commit/afca0e5e0e5555f54256df269c6d9621b6ca9be1)
- fixes 'unknown' media type from java plugin [`58ae85b`](https://github.com/datasonnet/datasonnet-mapper/commit/58ae85bc2e84bbacd4a34355a2418f76db3e1ac1)

#### [2.1.0](https://github.com/datasonnet/datasonnet-mapper/compare/2.0.2...2.1.0)

> 2 December 2020

- Import most recent MS3 additions [`#79`](https://github.com/datasonnet/datasonnet-mapper/pull/79)
- Add tests for nested namespaces [`#78`](https://github.com/datasonnet/datasonnet-mapper/pull/78)
- Add function for flattening XML content [`#77`](https://github.com/datasonnet/datasonnet-mapper/pull/77)
- header default input/output through quality param [`#76`](https://github.com/datasonnet/datasonnet-mapper/pull/76)
- Change XML node ordering to be based on ordering keys [`#73`](https://github.com/datasonnet/datasonnet-mapper/pull/73)
- Updated Header to support minor versions [`#69`](https://github.com/datasonnet/datasonnet-mapper/pull/69)
- Add support for comments in headers [`#67`](https://github.com/datasonnet/datasonnet-mapper/pull/67)
- Make library creation more java friendly [`#62`](https://github.com/datasonnet/datasonnet-mapper/pull/62)
- re-enable mappertest [`8bd4f27`](https://github.com/datasonnet/datasonnet-mapper/commit/8bd4f27571782501ec7d0ae0fc96b6e4b3fa51e2)
- Added encrypt and decrypt functions. RSA disabled currently [`9ce23ca`](https://github.com/datasonnet/datasonnet-mapper/commit/9ce23ca8e47483f1c9e982e4d611dde5b1077ea3)
- Refactor encrypt/decrypt to support arbitrary transformation [`6135833`](https://github.com/datasonnet/datasonnet-mapper/commit/61358335ff41fc98cba5f5e0added1a4b898e8cf)
