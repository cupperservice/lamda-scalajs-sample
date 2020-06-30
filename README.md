# Scala.js AWS Lambda sample

# Development

Watch for changes and re-compile (from sbt):
```
> ~fastOptJS::webpack
```

# Deploy

Package the lambda (from sbt):
```
> universal:packageBin
```

Deploy the resulting zip in `target/universal` to AWS Lambda. The handler name will be `${project-name}.handler`.

# Template license

Written in 2020 by Bryan Gahagan 

To the extent possible under law, the author(s) have dedicated all copyright and related
and neighboring rights to this template to the public domain worldwide.
This template is distributed without any warranty. See <https://creativecommons.org/publicdomain/zero/1.0/>.
